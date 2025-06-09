@Bean
@DependsOn("redisConnectionFactory")
public CommandLineRunner testRedisConnection(RedisConnectionFactory connectionFactory) {
    return args -> {
        try {
            RedisConnection connection = connectionFactory.getConnection();
            connection.ping();
            System.out.println("Redis connection successful!");
            connection.close();
        } catch (Exception e) {
            System.err.println("Redis connection failed: " + e.getMessage());
        }
    };
}
