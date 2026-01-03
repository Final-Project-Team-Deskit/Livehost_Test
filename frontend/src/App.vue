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
        <label>Broadcast ID</label>
        <input v-model="broadcastId" class="form-control" type="text" required />
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
        <h1 id="session-title">{{ sessionTitle }}</h1>
        <input class="btn btn-large btn-danger" type="button" id="buttonLeaveSession" @click="leaveSession"
               value="Leave session" />
      </div>

      <div id="main-video" class="col-md-12">
        <user-video :stream-manager="mainStreamManager" />
      </div>

      <div class="row">
        <div class="col-md-4">
          <h3>Broadcast Info</h3>
          <ul v-if="broadcastInfo">
            <li><strong>Title:</strong> {{ broadcastInfo.broadcastTitle }}</li>
            <li><strong>Notice:</strong> {{ broadcastInfo.broadcastNotice }}</li>
            <li><strong>Status:</strong> {{ broadcastInfo.status }}</li>
          </ul>
          <p v-else class="text-muted">Loading broadcast info...</p>
        </div>
        <div class="col-md-4">
          <h3>Live Stats</h3>
          <ul>
            <li>Viewers: {{ stats.viewerCount }}</li>
            <li>Likes: {{ stats.likeCount }}</li>
            <li>Reports: {{ stats.reportCount }}</li>
          </ul>
        </div>
        <div class="col-md-4">
          <h3>Products</h3>
          <ul v-if="products.length">
            <li v-for="product in products" :key="product.bpId">
              <strong>[{{ product.isPinned ? 'PINNED' : 'LIVE' }}]</strong> {{ product.name }} (₩{{ product.bpPrice }}) - 재고 {{ product.bpQuantity }}
            </li>
          </ul>
          <p v-else class="text-muted">No products loaded.</p>
        </div>
      </div>

      <div class="row">
        <div class="col-md-12">
          <h3>Live Notifications</h3>
          <ul v-if="eventLogs.length">
            <li v-for="(log, index) in eventLogs" :key="index">
              [{{ log.time }}] {{ log.type }} - {{ log.message }}
            </li>
          </ul>
          <p v-else class="text-muted">Waiting for notifications...</p>
        </div>
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
      broadcastId: "",
      sessionTitle: "",
      myUserName: "User" + Math.floor(Math.random() * 100),
      myRole: "SUBSCRIBER", // 기본값은 시청자
      viewerId: `viewer-${Math.floor(Math.random() * 100000)}`,
      broadcastInfo: null,
      stats: {
        viewerCount: 0,
        likeCount: 0,
        reportCount: 0,
      },
      products: [],
      eventLogs: [],
      eventSource: null,
      polling: {
        stats: null,
        products: null,
      },
    };
  },

  methods: {
    async joinSession() {
      if (!this.broadcastId) {
        alert('Broadcast ID를 입력해주세요.');
        return;
      }

      this.sessionTitle = `broadcast-${this.broadcastId}`;
      this.OV = new OpenVidu();
      this.session = this.OV.initSession();

      this.session.on("streamCreated", ({ stream }) => {
        const subscriber = this.session.subscribe(stream, undefined);
        this.subscribers.push(subscriber);
        if (this.myRole === 'SUBSCRIBER' && !this.mainStreamManager) {
          this.mainStreamManager = subscriber;
        }
      });

      this.session.on("streamDestroyed", ({ stream }) => {
        const index = this.subscribers.indexOf(stream.streamManager, 0);
        if (index >= 0) {
          this.subscribers.splice(index, 1);
        }
      });

      this.session.on("exception", ({ exception }) => {
        console.warn(exception);
      });

      try {
        await this.loadBroadcastInfo();
        const token = await this.fetchToken();

        await this.session.connect(token, { clientData: this.myUserName });

        if (this.myRole === 'PUBLISHER') {
          const publisher = this.OV.initPublisher(undefined, {
            audioSource: undefined,
            videoSource: undefined,
            publishAudio: true,
            publishVideo: true,
            resolution: "640x480",
            frameRate: 30,
            insertMode: "APPEND",
            mirror: false,
          });

          publisher.on('streamPlaying', () => {
            console.log("📺 영상 송출 시작됨! 이제 녹화를 요청합니다.");
            this.startRecording(this.sessionTitle);
          });

          this.mainStreamManager = publisher;
          this.publisher = publisher;
          this.session.publish(this.publisher);
        }
        this.startPolling();
        this.startSse();
      } catch (error) {
        console.log("Error connecting to session:", error?.code || '', error?.message || error);
        alert('세션 연결에 실패했습니다.');
      }

      window.addEventListener("beforeunload", this.leaveSession);
    },

    leaveSession() {
      // [핵심] 3. 방송을 끌 때 녹화 종료 API도 같이 호출
      if (this.myRole === 'PUBLISHER' && this.session) {
        this.stopRecording(this.sessionTitle);
      }
      if (this.session) this.session.disconnect();
      this.session = undefined;
      this.mainStreamManager = undefined;
      this.publisher = undefined;
      this.subscribers = [];
      this.broadcastInfo = null;
      this.stats = { viewerCount: 0, likeCount: 0, reportCount: 0 };
      this.products = [];
      this.eventLogs = [];
      this.OV = undefined;
      this.stopPolling();
      this.stopSse();
      window.removeEventListener("beforeunload", this.leaveSession);
    },

    async loadBroadcastInfo() {
      const path = this.myRole === 'PUBLISHER'
          ? `seller/api/broadcasts/${this.broadcastId}`
          : `api/broadcasts/${this.broadcastId}`;
      const response = await axios.get(APPLICATION_SERVER_URL + path);
      this.broadcastInfo = response.data.data;
    },

    async fetchStats() {
      if (!this.broadcastId) return;
      const response = await axios.get(APPLICATION_SERVER_URL + `api/broadcasts/${this.broadcastId}/stats`);
      this.stats = response.data.data || { viewerCount: 0, likeCount: 0, reportCount: 0 };
    },

    async fetchProducts() {
      if (!this.broadcastId) return;
      const response = await axios.get(APPLICATION_SERVER_URL + `api/broadcasts/${this.broadcastId}/products`);
      this.products = response.data.data || [];
    },

    startPolling() {
      this.stopPolling();
      this.fetchStats();
      this.fetchProducts();
      this.polling.stats = setInterval(() => this.fetchStats(), 5000);
      this.polling.products = setInterval(() => this.fetchProducts(), 7000);
    },

    stopPolling() {
      if (this.polling.stats) clearInterval(this.polling.stats);
      if (this.polling.products) clearInterval(this.polling.products);
      this.polling.stats = null;
      this.polling.products = null;
    },

    startSse() {
      this.stopSse();
      if (!this.broadcastId) return;

      const sseUrl = `${APPLICATION_SERVER_URL}api/broadcasts/${this.broadcastId}/subscribe?viewerId=${encodeURIComponent(this.viewerId)}`;
      this.eventSource = new EventSource(sseUrl);

      this.eventSource.onmessage = (event) => {
        this.pushEventLog(event.type || 'message', event.data);
      };

      this.eventSource.addEventListener('PRODUCT_PINNED', (event) => {
        this.pushEventLog('PRODUCT_PINNED', event.data);
        this.fetchProducts();
      });

      this.eventSource.addEventListener('BROADCAST_UPDATED', (event) => {
        this.pushEventLog('BROADCAST_UPDATED', event.data);
        this.loadBroadcastInfo();
      });

      this.eventSource.addEventListener('BROADCAST_ENDED', (event) => {
        this.pushEventLog('BROADCAST_ENDED', event.data);
        this.fetchStats();
      });

      this.eventSource.onerror = () => {
        this.pushEventLog('ERROR', 'SSE connection lost, retrying...');
        this.stopSse();
        setTimeout(() => this.startSse(), 3000);
      };
    },

    stopSse() {
      if (this.eventSource) {
        this.eventSource.close();
        this.eventSource = null;
      }
    },

    pushEventLog(type, message) {
      const time = new Date().toLocaleTimeString();
      this.eventLogs.unshift({ type, message, time });
      if (this.eventLogs.length > 20) {
        this.eventLogs.pop();
      }
    },

    async fetchToken() {
      if (this.myRole === 'PUBLISHER') {
        const response = await axios.post(
            APPLICATION_SERVER_URL + `seller/api/broadcasts/${this.broadcastId}/start`
        );
        return response.data.data;
      }

      const response = await axios.post(
          APPLICATION_SERVER_URL + `api/broadcasts/${this.broadcastId}/join`,
          null,
          this.viewerId ? { headers: { 'X-Viewer-Id': this.viewerId } } : undefined,
      );
      return response.data.data;
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
        const response = await axios.post(
            APPLICATION_SERVER_URL + 'api/sessions/' + sessionId + '/recording/stop',
            null
        );
        console.log("✅ 녹화 종료 & VOD URL 생성 완료:", response.data);
      } catch (error) {
        console.error("❌ 녹화 종료 실패:", error);
      }
    },
  },
};
</script>
