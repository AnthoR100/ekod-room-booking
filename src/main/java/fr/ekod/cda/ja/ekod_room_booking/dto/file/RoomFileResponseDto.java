package fr.ekod.cda.ja.ekod_room_booking.dto.file;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RoomFileResponseDto {

    private Long id;
    private String filename;
    private String contentType;
    private long size;
    private LocalDateTime createdAt;
    private String downloadUrl;
}
