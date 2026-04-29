package com.yas.media.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yas.media.model.Media;
import com.yas.media.model.dto.MediaDto;
import com.yas.media.service.MediaService;
import com.yas.media.viewmodel.MediaPostVm;
import com.yas.media.viewmodel.MediaVm;
import java.io.ByteArrayInputStream;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MediaController.class)
@AutoConfigureMockMvc(addFilters = false)
class MediaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MediaService mediaService;

    private Media media;
    private MediaVm mediaVm;
    private MediaDto mediaDto;

    @BeforeEach
    void setUp() {
        media = new Media();
        media.setId(1L);
        media.setCaption("test caption");
        media.setFileName("test.png");
        media.setMediaType("image/png");

        mediaVm = new MediaVm(1L, "test caption", "test.png", "image/png", "url");

        mediaDto = new MediaDto();
        mediaDto.setContent(new ByteArrayInputStream("test data".getBytes()));
        mediaDto.setMediaType(MediaType.IMAGE_PNG);
    }

    @Test
    void create_ValidRequest_ReturnsOk() throws Exception {
        MockMultipartFile file = new MockMultipartFile("multipartFile", "test.png", "image/png", "test data".getBytes());

        when(mediaService.saveMedia(any(MediaPostVm.class))).thenReturn(media);

        mockMvc.perform(multipart("/medias")
                .file(file)
                .param("caption", "test caption")
                .param("fileNameOverride", "test.png")
                .contentType(MediaType.MULTIPART_FORM_DATA))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.caption").value("test caption"))
            .andExpect(jsonPath("$.fileName").value("test.png"));
    }

    @Test
    void delete_ValidId_ReturnsNoContent() throws Exception {
        doNothing().when(mediaService).removeMedia(1L);

        mockMvc.perform(delete("/medias/{id}", 1L))
            .andExpect(status().isNoContent());
    }

    @Test
    void get_ValidId_ReturnsMedia() throws Exception {
        when(mediaService.getMediaById(1L)).thenReturn(mediaVm);

        mockMvc.perform(get("/medias/{id}", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.caption").value("test caption"));
    }

    @Test
    void get_InvalidId_ReturnsNotFound() throws Exception {
        when(mediaService.getMediaById(anyLong())).thenReturn(null);

        mockMvc.perform(get("/medias/{id}", 99L))
            .andExpect(status().isNotFound());
    }

    @Test
    void getByIds_ValidIds_ReturnsMediaList() throws Exception {
        when(mediaService.getMediaByIds(List.of(1L))).thenReturn(List.of(mediaVm));

        mockMvc.perform(get("/medias").param("ids", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    void getByIds_InvalidIds_ReturnsNotFound() throws Exception {
        when(mediaService.getMediaByIds(any())).thenReturn(List.of());

        mockMvc.perform(get("/medias").param("ids", "99"))
            .andExpect(status().isNotFound());
    }

    @Test
    void getFile_ValidId_ReturnsFile() throws Exception {
        when(mediaService.getFile(1L, "test.png")).thenReturn(mediaDto);

        mockMvc.perform(get("/medias/{id}/file/{fileName}", 1L, "test.png"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"test.png\""))
            .andExpect(content().bytes("test data".getBytes()));
    }
}
