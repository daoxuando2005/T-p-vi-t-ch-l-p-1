package com.hocchu.web.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint cho client kết nối WebSocket, cho phép CORS từ mọi origin
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Tiền tố cho các kênh nhận tin nhắn
        registry.enableSimpleBroker("/topic", "/queue", "/user");
        
        // Tiền tố cho các request gửi lên server xử lý (@MessageMapping)
        registry.setApplicationDestinationPrefixes("/app");
        
        // Cấu hình định tuyến tin nhắn cá nhân
        registry.setUserDestinationPrefix("/user");
    }
}
