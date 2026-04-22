export const config = {
  port: parseInt(process.env.PORT || '8080', 10),
  kafkaBrokers: (process.env.KAFKA_BROKERS || 'localhost:9092').split(','),
  redisUrl: process.env.REDIS_URL || 'redis://localhost:6379',
  serviceName: process.env.OTEL_SERVICE_NAME || 'notification-service',
};
