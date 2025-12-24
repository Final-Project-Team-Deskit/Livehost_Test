package com.example.LiveHost.controller;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import io.openvidu.java.client.*;
import jakarta.annotation.PostConstruct;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.net.ssl.*;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@CrossOrigin(origins = "*") // 어떤 도메인에서든 애플리케이션에 접근 가능, 추후 수정
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LiveHostController {
    @Value("${openvidu.url}")
    private String OPENVIDU_URL;

    @Value("${openvidu.secret}")
    private String OPENVIDU_SECRET;

    private OpenVidu openvidu;

    // 세션 ID와 녹화 ID를 매핑해서 관리 (방송 종료 시 녹화를 멈추기 위해)
    private Map<String, String> sessionRecordings = new ConcurrentHashMap<>();

    // [NCP 설정 값 주입]

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;
    @Value("${cloud.aws.s3.endpoint}")
    private String endpoint;

    private final AmazonS3 amazonS3;

    @PostConstruct
    public void init() {
        // 1. OpenVidu 객체 초기화 (미디어 서버와 연결 준비)
        this.openvidu = new OpenVidu(OPENVIDU_URL, OPENVIDU_SECRET);
    }

    // 1. 방송 방(session) 만들기
    @PostMapping("/sessions")
    public ResponseEntity<String> initializeSession(@RequestBody(required = false) Map<String, Object> params)
            throws OpenViduJavaClientException, OpenViduHttpException {
        // 녹화 설정
        RecordingProperties recordingProperties = new RecordingProperties.Builder()
                .outputMode(Recording.OutputMode.COMPOSED) // 방송 화면 그대로(하나의 비디오) 녹화
                .hasAudio(true)
                .hasVideo(true)
                .build();

        // 세션 생성, 레코딩 설정 추가
        SessionProperties properties = new SessionProperties.Builder()
                .recordingMode(RecordingMode.MANUAL) // 수동 녹화
                .defaultRecordingProperties(recordingProperties)
                .customSessionId((String) params.get("customSessionId"))
                .build();

        Session session = openvidu.createSession(properties);

        //세션 ID 반환
        return new ResponseEntity<>(session.getSessionId(), HttpStatus.OK);
    }

    // 2. 연결 생성(입장권 만들기, 토큰 반환)
    @PostMapping("/sessions/{sessionId}/connections")
    public ResponseEntity<String> createConnection(@PathVariable("sessionId") String sessionId,
                                                   @RequestBody(required = false) Map<String, Object> params)
            throws OpenViduJavaClientException, OpenViduHttpException {

        Session session = this.openvidu.getActiveSession(sessionId);
        if (session == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        // 클라이언트에서 요청한 role 값을 확인
        boolean isHost = params != null && params.get("role") != null && "PUBLISHER".equals(params.get("role"));
        OpenViduRole role = isHost ? OpenViduRole.PUBLISHER : OpenViduRole.SUBSCRIBER;

        // 연결 속성에 Role 설정
        ConnectionProperties properties = new ConnectionProperties.Builder()
                .type(ConnectionType.WEBRTC)
                .role(role) // 설정된 Role 적용
                .data(params != null ? (String) params.get("clientData") : "")
                .build();

        Connection connection = session.createConnection(properties);

        return new ResponseEntity<>(connection.getToken(), HttpStatus.OK);
    }

    // 3. 녹화 시작 (방송 시작 버튼 클릭 시 호출)
    @PostMapping("/sessions/{sessionId}/recording/start")
    public ResponseEntity<String> startRecording(@PathVariable("sessionId") String sessionId) {
        try {
            Recording recording = this.openvidu.startRecording(sessionId);
            this.sessionRecordings.put(sessionId, recording.getId()); // 나중에 멈추기 위해 저장
            return new ResponseEntity<>(recording.getId(), HttpStatus.OK);
        } catch (OpenViduJavaClientException | OpenViduHttpException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 4. 녹화 종료 (방송 종료 버튼 클릭 시 호출)
    @PostMapping("/sessions/{sessionId}/recording/stop")
    public ResponseEntity<String> stopRecording(@RequestHeader("X-Seller-Id") Long sellerId, // Gateway 등에서 넘어온 판매자 ID
            @PathVariable("sessionId") String sessionId) {
        String recordingId = this.sessionRecordings.remove(sessionId);
        if (recordingId == null) {
            return new ResponseEntity<>("Recording not found for session", HttpStatus.NOT_FOUND);
        }

        try {
            // [중요] OpenVidu 서버 접속 전 SSL 검증 끄기
            disableSslVerification();
            // A. 녹화 중단 명령 (OpenVidu 서버가 녹화를 멈춤)
            Recording recording = this.openvidu.stopRecording(recordingId);

            // B. OpenVidu 서버에서 파일을 가져오기 위한 URL 생성
            // 즉, OpenVidu 서버가 가지고 있는 녹화 파일의 웹 주소를 만드는 것
            // (OpenVidu는 녹화 파일을 https://DOMAIN/openvidu/recordings/... 경로로 제공)
            // OPENVIDU_URL 뒤에 슬래시(/)가 있는지 없는지에 따라 주소 조합을 조절
            String videoUrl = OPENVIDU_URL.replaceAll("/$", "") +
                    "/openvidu/recordings/" + recordingId + "/" + recordingId + ".mp4";

            // C. OpenVidu 서버로 HTTP 연결 연결 (파일 다운로드 준비)
            URL url = new URL(videoUrl); // 자바가 이해하고 사용할 수 있는 주소 객체로 문자열 주소를 바꾸는 과정
            HttpURLConnection conn = (HttpURLConnection) url.openConnection(); // 해당 주소로 연결할 수 있는 연결 객체를 만듦
            conn.setRequestMethod("GET"); // OpenVidu 서버에 있는 영상 파일을 다운로드해야 하므로 GET 방식을 선택

            // OpenVidu 서버 접근 인증 (Basic Auth)
            // 즉 인증 헤더 만들기
            String auth = "OPENVIDUAPP:" + OPENVIDU_SECRET; // 아이디 및 비밀번호
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
            // HTTP 통신 규약상, 아이디와 비번을 ID:PW 형태로 합치고 이를 암호화(인코딩)해서 헤더에 실어 보냄
            conn.setRequestProperty("Authorization", "Basic " + encodedAuth);

            // 연결 코드가 200(성공)인지 확인
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {

                // D. 스트림을 열어서 NCP(S3)로 바로 쏘기
                try (InputStream inputStream = conn.getInputStream()) {

                    // S3에 스트림으로 올릴 때는 메타데이터(파일 크기 등)가 필요함
                    ObjectMetadata metadata = new ObjectMetadata();
                    metadata.setContentLength(conn.getContentLengthLong()); // OpenVidu 서버가 헤더로 알려준 정보를 NCP한테 전달하는 것
                    metadata.setContentType("video/mp4");

                    String objectName = "seller_" + sellerId + "/vods/" + recordingId + ".mp4";

                    // [업로드 실행] OpenVidu -> (Stream) -> Backend -> NCP
                    amazonS3.putObject(new PutObjectRequest(bucketName, objectName, inputStream, metadata)
                            .withCannedAcl(CannedAccessControlList.PublicRead));

                    // E. 업로드 완료된 URL 반환
                    String fileUrl = endpoint + "/" + bucketName + "/" + objectName;
                    return new ResponseEntity<>(fileUrl, HttpStatus.OK);
                }
            } else {
                return new ResponseEntity<>("Failed to fetch recording from OpenVidu Server: " + responseCode, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }


    }

    // [신규] SSL 인증서 무시 설정 (OpenVidu 자가 서명 인증서 허용)
    private void disableSslVerification() {
        try {
            // 모든 인증서를 신뢰하는 TrustManager 생성
            TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
                public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                public void checkServerTrusted(X509Certificate[] certs, String authType) { }
            } };

            // SSLContext 초기화
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            // 모든 호스트 이름 허용 (Verifier 무력화)
            HostnameVerifier allHostsValid = (hostname, session) -> true;
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            e.printStackTrace();
        }
    }
}
