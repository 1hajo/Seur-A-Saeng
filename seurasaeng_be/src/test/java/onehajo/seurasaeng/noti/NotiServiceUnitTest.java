package onehajo.seurasaeng.noti;

import onehajo.seurasaeng.entity.Noti;
import onehajo.seurasaeng.newnoti.repository.NewnotiRepository;
import onehajo.seurasaeng.noti.dto.NotiResDTO;
import onehajo.seurasaeng.noti.repository.NotiRepository;
import onehajo.seurasaeng.noti.service.NotiService;
import onehajo.seurasaeng.popup.repository.PopupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotiServiceUnitTest {

    @InjectMocks
    private NotiService notiService;

    @Mock private NotiRepository notiRepository;
    @Mock private NewnotiRepository newnotiRepository;
    @Mock private PopupRepository popupRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void searchNoti() throws Exception {
        Noti noti = Noti.builder()
                .id(1L).title("title").content("content").created_at(LocalDateTime.now()).build();

        when(notiRepository.findById(1L)).thenReturn(Optional.of(noti));

        NotiResDTO result = notiService.read(1L);
        assertEquals("title", result.getTitle());
    }

    @Test
    void deleteNoti() throws Exception {
        Noti noti = Noti.builder().id(1L).build();
        when(notiRepository.findById(1L)).thenReturn(Optional.of(noti));
        when(popupRepository.findAll()).thenReturn(List.of());
        when(newnotiRepository.findAll()).thenReturn(List.of());

        notiService.delete(1L);

        verify(notiRepository).delete(noti);
    }
}
