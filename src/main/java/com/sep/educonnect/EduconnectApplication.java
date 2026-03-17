package com.sep.educonnect;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchRepositoriesAutoConfiguration;

@SpringBootApplication(exclude = {
		ElasticsearchDataAutoConfiguration.class,
		ElasticsearchRepositoriesAutoConfiguration.class
})
public class EduconnectApplication {
	public static void main(String[] args) {
		SpringApplication.run(EduconnectApplication.class, args);
	}

    @PostConstruct
    public void logWebClientRuntime() {
        System.out.println("WebClient runtime = " +
                reactor.netty.http.client.HttpClient.class.getName());
    }

}
