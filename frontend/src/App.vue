<template>
  <div id="main-container" class="container">
    <div id="join" v-if="!session">
      <div id="img-div">
        <img src="resources/images/openvidu_grey_bg_transp_cropped.png" />
      </div>
      <div id="join-dialog" class="jumbotron vertical-center">
        <h1>Live Commerce Test</h1>
        <div class="form-group">
          <p>
            <label>Participant</label>
            <input v-model="myUserName" class="form-control" type="text" required />
          </p>
          <p>
            <label>Session</label>
            <input v-model="mySessionId" class="form-control" type="text" required />
          </p>

          <p>
            <label>Role</label>
            <select v-model="myRole" class="form-control">
              <option value="PUBLISHER">Host (방송하기)</option>
              <option value="SUBSCRIBER">Viewer (시청하기)</option>
            </select>
          </p>

          <p class="text-center">
            <button class="btn btn-lg btn-success" @click="joinSession()">
              Join!
            </button>
          </p>
        </div>
      </div>
    </div>

    <div id="session" v-if="session">
      <div id="session-header">
        <h1 id="session-title">{{ mySessionId }}</h1>
        <input class="btn btn-large btn-danger" type="button" id="buttonLeaveSession" @click="leaveSession"
               value="Leave session" />
      </div>

      <div id="main-video" class="col-md-12">
        <user-video :stream-manager="mainStreamManager" />
      </div>

    </div>
  </div>
</template>

<script>
import axios from "axios";
import { OpenVidu } from "openvidu-browser";
import UserVideo from "./components/UserVideo";

axios.defaults.headers.post["Content-Type"] = "application/json";

const APPLICATION_SERVER_URL = process.env.NODE_ENV === 'production' ? '' : 'http://localhost:8080/';

export default {
  name: "App",
  components: { UserVideo },
  data() {
    return {
      OV: undefined,
      session: undefined,
      mainStreamManager: undefined,
      publisher: undefined,
      subscribers: [],
      mySessionId: "LiveSession1",
      myUserName: "User" + Math.floor(Math.random() * 100),
      myRole: "SUBSCRIBER", // 기본값은 시청자
    };
  },

  methods: {
    joinSession() {
      this.OV = new OpenVidu();
      this.session = this.OV.initSession();

      // 스트림이 생성되면(방송이 시작되면) 구독
      this.session.on("streamCreated", ({ stream }) => {
        const subscriber = this.session.subscribe(stream, undefined);
        this.subscribers.push(subscriber);
        // 시청자라면 들어오자마자 방송 화면을 메인으로 설정
        if (this.myRole === 'SUBSCRIBER') {
          this.mainStreamManager = subscriber;
        }
      });

      this.session.on("streamDestroyed", ({ stream }) => {
        const index = this.subscribers.indexOf(stream.streamManager, 0);
        if (index >= 0) {
          this.subscribers.splice(index, 1);
        }
        // 방송이 종료되면 메인 화면 초기화 로직 필요
      });

      this.session.on("exception", ({ exception }) => {
        console.warn(exception);
      });

      // 토큰 발급 시 역할(Role) 정보 함께 전달
      this.getToken(this.mySessionId, this.myRole).then((token) => {
        this.session.connect(token, { clientData: this.myUserName })
            .then(() => {

              // [중요] Host일 경우에만 영상을 송출(Publish)
              if (this.myRole === 'PUBLISHER') {
                let publisher = this.OV.initPublisher(undefined, {
                  audioSource: undefined,
                  videoSource: undefined,
                  publishAudio: true,
                  publishVideo: true,
                  resolution: "640x480",
                  frameRate: 30,
                  insertMode: "APPEND",
                  mirror: false,
                });

                // [수정] 바로 startRecording 하지 말고, 이벤트 리스너 등록
                publisher.on('streamPlaying', () => {
                  console.log("📺 영상 송출 시작됨! 이제 녹화를 요청합니다.");
                  this.startRecording(this.mySessionId);
                });

                this.mainStreamManager = publisher;
                this.publisher = publisher;
                this.session.publish(this.publisher);
                console.log(publisher);

                // [핵심] 2. 방송이 시작되었으니, 즉시 녹화 시작 API를 호출합니다. (자동화)
                // this.startRecording(this.mySessionId);
              }
            })
            .catch((error) => {
              console.log("Error connecting to session:", error.code, error.message);
            });
      });

      window.addEventListener("beforeunload", this.leaveSession);
    },

    leaveSession() {
      // [핵심] 3. 방송을 끌 때 녹화 종료 API도 같이 호출
      if (this.myRole === 'PUBLISHER' && this.session) {
        this.stopRecording(this.mySessionId);
      }
      if (this.session) this.session.disconnect();
      this.session = undefined;
      this.mainStreamManager = undefined;
      this.publisher = undefined;
      this.subscribers = [];
      this.OV = undefined;
      window.removeEventListener("beforeunload", this.leaveSession);
    },

    // 토큰 생성 시 role 파라미터 추가
    async getToken(mySessionId, role) {
      const sessionId = await this.createSession(mySessionId);
      return await this.createToken(sessionId, role);
    },

    async createSession(sessionId) {
      const response = await axios.post(APPLICATION_SERVER_URL + 'api/sessions', { customSessionId: sessionId });
      return response.data;
    },

    // [수정] 토큰 및 role 정보를 백엔드로 전송
    async createToken(sessionId, role) {
      const response = await axios.post(APPLICATION_SERVER_URL + 'api/sessions/' + sessionId + '/connections',
          { role: role }, // body에 role 추가
          { headers: { 'Content-Type': 'application/json', } }
      );
      return response.data;
    },

    // [신규 추가] 녹화 시작 요청 함수
    async startRecording(sessionId) {
      try {
        // 백엔드 컨트롤러의 startRecording API 호출
        await axios.post(APPLICATION_SERVER_URL + 'api/sessions/' + sessionId + '/recording/start');
        console.log("✅ VOD 녹화가 자동으로 시작되었습니다.");
      } catch (error) {
        console.error("❌ 녹화 시작 실패:", error);
      }
    },

    // [신규 추가] 녹화 종료 요청 함수
    async stopRecording(sessionId) {
      try {
        // 백엔드 컨트롤러의 stopRecording API 호출
        const response = await axios.post(APPLICATION_SERVER_URL + 'api/sessions/' + sessionId + '/recording/stop');
        console.log("✅ 녹화 종료 & VOD URL 생성 완료:", response.data);
      } catch (error) {
        console.error("❌ 녹화 종료 실패:", error);
      }
    },
  },
};
</script>