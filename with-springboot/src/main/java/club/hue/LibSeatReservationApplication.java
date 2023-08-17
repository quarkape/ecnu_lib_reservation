package club.hue;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LibSeatReservationApplication {
    public static void main(String[] args) {
        SpringApplication.run(LibSeatReservationApplication.class, args);
    }
}
