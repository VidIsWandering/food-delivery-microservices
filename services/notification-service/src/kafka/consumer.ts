import { Kafka, Consumer, EachMessagePayload } from 'kafkajs';
import { SSEManager } from '../sse/manager';

/**
 * Kafka consumer that listens to order/payment/delivery events
 * and pushes real-time updates to connected clients via SSE.
 */
export class KafkaConsumer {
  private kafka: Kafka;
  private consumer: Consumer;
  private sseManager: SSEManager;

  constructor(brokers: string[], sseManager: SSEManager) {
    this.kafka = new Kafka({
      clientId: 'notification-service',
      brokers,
    });
    this.consumer = this.kafka.consumer({ groupId: 'notification-service-group' });
    this.sseManager = sseManager;
  }

  async connect(): Promise<void> {
    await this.consumer.connect();

    // Subscribe to all event topics
    await this.consumer.subscribe({ topic: 'order-events', fromBeginning: false });
    await this.consumer.subscribe({ topic: 'payment-events', fromBeginning: false });
    await this.consumer.subscribe({ topic: 'delivery-events', fromBeginning: false });
    await this.consumer.subscribe({ topic: 'restaurant-events', fromBeginning: false });

    await this.consumer.run({
      eachMessage: async (payload: EachMessagePayload) => {
        await this.handleMessage(payload);
      },
    });

    console.log('Kafka consumer connected and listening');
  }

  async disconnect(): Promise<void> {
    await this.consumer.disconnect();
  }

  private async handleMessage({ topic, message }: EachMessagePayload): Promise<void> {
    if (!message.value) return;

    try {
      const event = JSON.parse(message.value.toString());
      const eventType = event.type as string;
      const orderId = event.data?.order_id as string;

      if (!orderId) {
        console.warn('Event missing order_id', { topic, eventType });
        return;
      }

      console.log('Processing event', { topic, eventType, orderId });

      // Map event type to notification message
      const notification = this.mapEventToNotification(eventType, event.data);
      if (notification) {
        this.sseManager.send(orderId, notification);
      }
    } catch (err) {
      console.error('Failed to process Kafka message', { topic, error: err });
    }
  }

  private mapEventToNotification(
    eventType: string,
    data: Record<string, unknown>,
  ): { status: string; message: string; data?: unknown } | null {
    switch (eventType) {
      case 'OrderCreated':
        return { status: 'CREATED', message: 'Order has been created' };
      case 'PaymentSuccess':
        return { status: 'PAID', message: 'Payment completed successfully' };
      case 'PaymentFailed':
        return { status: 'PAYMENT_FAILED', message: 'Payment failed', data };
      case 'OrderAccepted':
        return { status: 'ACCEPTED', message: 'Restaurant accepted the order' };
      case 'OrderRejected':
        return { status: 'REJECTED', message: 'Restaurant rejected the order', data };
      case 'OrderReadyForPickup':
        return { status: 'READY', message: 'Order is ready for pickup' };
      case 'DriverAssigned':
        return { status: 'DRIVER_ASSIGNED', message: `Driver ${data.driver_name} is on the way`, data };
      case 'DriverPickedUp':
        return { status: 'PICKED_UP', message: 'Driver picked up the order' };
      case 'OrderDelivered':
        return { status: 'DELIVERED', message: 'Order delivered successfully' };
      case 'OrderCancelled':
        return { status: 'CANCELLED', message: 'Order has been cancelled', data };
      case 'PaymentRefunded':
        return { status: 'REFUNDED', message: 'Payment has been refunded' };
      default:
        return null;
    }
  }
}
