/**
 * SSEManager manages active Server-Sent Event connections per order ID.
 * When a Kafka event arrives, it pushes the update to the connected client.
 */
export class SSEManager {
  private connections: Map<string, Set<(data: string) => void>> = new Map();

  /**
   * Register a new SSE connection for an order.
   */
  addConnection(orderId: string, sendFn: (data: string) => void): void {
    if (!this.connections.has(orderId)) {
      this.connections.set(orderId, new Set());
    }
    this.connections.get(orderId)!.add(sendFn);
  }

  /**
   * Remove a connection (client disconnected).
   */
  removeConnection(orderId: string, sendFn: (data: string) => void): void {
    const conns = this.connections.get(orderId);
    if (conns) {
      conns.delete(sendFn);
      if (conns.size === 0) {
        this.connections.delete(orderId);
      }
    }
  }

  /**
   * Send an event to all clients watching a specific order.
   */
  send(orderId: string, event: { status: string; message: string; data?: unknown }): void {
    const conns = this.connections.get(orderId);
    if (conns) {
      const payload = JSON.stringify(event);
      conns.forEach((sendFn) => sendFn(payload));
    }
  }

  /**
   * Get count of active connections (for monitoring).
   */
  getActiveCount(): number {
    let count = 0;
    this.connections.forEach((conns) => (count += conns.size));
    return count;
  }
}
