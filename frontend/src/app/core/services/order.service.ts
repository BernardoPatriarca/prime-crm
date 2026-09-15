import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Order, OrderItem, OrderItemRequest, OrderRequest, OrderStatus, OrderStatusUpdateRequest } from '../models/order.model';
import { PageResponse } from '../models/page.model';
import { buildHttpParams } from '../utils/http-params.util';

export interface OrderListQuery {
  search?: string;
  status?: OrderStatus;
  customerId?: string;
  opportunityId?: string;
  ownerUserId?: string;
  page?: number;
  size?: number;
  sort?: string;
}

@Injectable({ providedIn: 'root' })
export class OrderService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/orders`;

  list(query: OrderListQuery): Observable<PageResponse<Order>> {
    return this.http.get<PageResponse<Order>>(this.baseUrl, { params: buildHttpParams({ ...query }) });
  }

  getById(id: string): Observable<Order> {
    return this.http.get<Order>(`${this.baseUrl}/${id}`);
  }

  create(request: OrderRequest): Observable<Order> {
    return this.http.post<Order>(this.baseUrl, request);
  }

  createFromProposal(proposalId: string): Observable<Order> {
    return this.http.post<Order>(`${this.baseUrl}/from-proposal/${proposalId}`, {});
  }

  update(id: string, request: OrderRequest): Observable<Order> {
    return this.http.put<Order>(`${this.baseUrl}/${id}`, request);
  }

  changeStatus(id: string, request: OrderStatusUpdateRequest): Observable<Order> {
    return this.http.patch<Order>(`${this.baseUrl}/${id}/status`, request);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  pdf(id: string): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/${id}/pdf`, { responseType: 'blob' });
  }

  listItems(orderId: string): Observable<OrderItem[]> {
    return this.http.get<OrderItem[]>(`${this.baseUrl}/${orderId}/items`);
  }

  createItem(orderId: string, request: OrderItemRequest): Observable<OrderItem> {
    return this.http.post<OrderItem>(`${this.baseUrl}/${orderId}/items`, request);
  }

  updateItem(orderId: string, itemId: string, request: OrderItemRequest): Observable<OrderItem> {
    return this.http.put<OrderItem>(`${this.baseUrl}/${orderId}/items/${itemId}`, request);
  }

  deleteItem(orderId: string, itemId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${orderId}/items/${itemId}`);
  }
}
