package com.ses.whodatidols.config;

import com.ses.whodatidols.repository.PersonRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final PersonRepository personRepository;

    public WebSocketConfig(PersonRepository personRepository) {
        this.personRepository = personRepository;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register endpoint with SockJS fallback options
        registry.addEndpoint("/ws")
                .addInterceptors(new WebSocketAuthInterceptor(personRepository))
                .setAllowedOriginPatterns("*")
                .withSockJS();

        // Register direct endpoint for standard WebSocket clients
        registry.addEndpoint("/ws")
                .addInterceptors(new WebSocketAuthInterceptor(personRepository))
                .setAllowedOriginPatterns("*");
    }
}
