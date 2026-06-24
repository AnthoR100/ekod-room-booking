package fr.ekod.cda.ja.ekod_room_booking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EkodRoomBookingApplication {

    public static void main(String[] args) {
        SpringApplication.run(EkodRoomBookingApplication.class, args);
    }

}
