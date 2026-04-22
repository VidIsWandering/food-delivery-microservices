import Fastify from 'fastify';
import { KafkaConsumer } from './kafka/consumer';
import { SSEManager } from './sse/manager';
import { config } from './config';
import { registerSSERoutes } from './sse/handler';

const app = Fastify({
  logger: {
    transport: {
      target: 'pino-pretty', // Replace with 'pino/file' in production for JSON
    },
  },
});

const sseManager = new SSEManager();

// Health check endpoints
app.get('/health/live', async () => ({ status: 'UP' }));
app.get('/health/ready', async () => ({ status: 'READY' }));

// SSE routes
registerSSERoutes(app, sseManager);

// Start Kafka consumer
const kafkaConsumer = new KafkaConsumer(config.kafkaBrokers, sseManager);

const start = async () => {
  try {
    await kafkaConsumer.connect();
    await app.listen({ port: config.port, host: '0.0.0.0' });
    console.log(`Notification service listening on port ${config.port}`);
  } catch (err) {
    app.log.error(err);
    process.exit(1);
  }
};

// Graceful shutdown
const shutdown = async () => {
  console.log('Shutting down...');
  await kafkaConsumer.disconnect();
  await app.close();
  process.exit(0);
};

process.on('SIGINT', shutdown);
process.on('SIGTERM', shutdown);

start();
