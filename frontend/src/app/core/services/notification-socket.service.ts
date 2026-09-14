import { Injectable, inject } from '@angular/core';
import { Client, IMessage } from '@stomp/stompjs';
import { Subject } from 'rxjs';
import { environment } from '../../../environments/environment';
import { NotificationList } from '../models/notification.model';
import { SessionStore } from '../store/session.store';

const RECONNECT_DELAY_MS = 5000;
const HEARTBEAT_MS = 10000;
const NOTIFICATIONS_DESTINATION = '/user/queue/notifications';

@Injectable({ providedIn: 'root' })
export class NotificationSocketService {
  private readonly sessionStore = inject(SessionStore);
  private readonly notifications$ = new Subject<NotificationList>();
  private client: Client | null = null;

  readonly notifications = this.notifications$.asObservable();

  connect(): void {
    if (this.client?.active) {
      return;
    }
    const token = this.sessionStore.accessToken();
    if (!token) {
      return;
    }

    this.client = new Client({
      brokerURL: this.buildBrokerUrl(token),
      reconnectDelay: RECONNECT_DELAY_MS,
      heartbeatIncoming: HEARTBEAT_MS,
      heartbeatOutgoing: HEARTBEAT_MS,
      onConnect: () => {
        this.client?.subscribe(NOTIFICATIONS_DESTINATION, (message: IMessage) => {
          this.notifications$.next(JSON.parse(message.body) as NotificationList);
        });
      }
    });
    this.client.activate();
  }

  disconnect(): void {
    this.client?.deactivate();
    this.client = null;
  }

  private buildBrokerUrl(token: string): string {
    const apiUrl = new URL(environment.apiBaseUrl);
    const scheme = apiUrl.protocol === 'https:' ? 'wss:' : 'ws:';
    return `${scheme}//${apiUrl.host}/ws/websocket?access_token=${encodeURIComponent(token)}`;
  }
}
