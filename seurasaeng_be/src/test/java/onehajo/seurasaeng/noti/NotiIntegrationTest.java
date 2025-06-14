package onehajo.seurasaeng.noti;

import com.fasterxml.jackson.databind.ObjectMapper;
import onehajo.seurasaeng.newnoti.service.NewnotiService;
import onehajo.seurasaeng.noti.dto.NotiReqDTO;
import onehajo.seurasaeng.noti.dto.NotiResDTO;
import onehajo.seurasaeng.noti.service.NotiService;
import onehajo.seurasaeng.popup.service.PopupService;
import onehajo.seurasaeng.user.service.UserService;
import onehajo.seurasaeng.util.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class NotiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private NotiService notiService;

    @MockBean
    private PopupService popupService;

    @MockBean
    private NewnotiService newnotiService;

    @MockBean
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("공지 작성")
    void testWriteNoti() throws Exception {
        NotiReqDTO req = new NotiReqDTO();
        // req에 필요한 필드 설정

        NotiResDTO res = new NotiResDTO();
        res.setId(1L);
        // res에 필요한 필드 설정

        Mockito.when(notiService.write(any(HttpServletRequest.class), any(NotiReqDTO.class))).thenReturn(res);
        Mockito.doNothing().when(newnotiService).write(eq(1L));

        mockMvc.perform(post("/api/notices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("전체 공지 조회")
    void testSearchAllNoti() throws Exception {
        List<NotiResDTO> list = Arrays.asList(new NotiResDTO(), new NotiResDTO());
        Mockito.when(notiService.readAll()).thenReturn(list);

        mockMvc.perform(get("/api/notices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("개별 공지 조회")
    void testSearchNoti() throws Exception {
        NotiResDTO res = new NotiResDTO();
        res.setId(1L);

        Mockito.when(notiService.read(eq(1L))).thenReturn(res);
        Mockito.when(jwtUtil.getRoleFromToken(anyString())).thenReturn("user");
        Mockito.doNothing().when(userService).readNoti(any(HttpServletRequest.class), eq(1L));

        mockMvc.perform(get("/api/notices/1")
                        .header("Authorization", "Bearer dummyToken"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @DisplayName("공지 삭제")
    void testDeleteNoti() throws Exception {
        Mockito.doNothing().when(notiService).delete(eq(1L));

        mockMvc.perform(delete("/api/notices/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("공지 팝업 설정")
    void testSetPopup() throws Exception {
        Mockito.doNothing().when(popupService).set(eq(1L));

        mockMvc.perform(post("/api/notices/1/popup"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("공지 팝업 조회")
    void testSearchPopup() throws Exception {
        NotiResDTO popup = new NotiResDTO();
        popup.setId(99L);
        Mockito.when(popupService.read()).thenReturn(popup);

        mockMvc.perform(get("/api/notices/popup"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(99L));
    }
}
