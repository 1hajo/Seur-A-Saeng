package onehajo.seurasaeng.Inquiry;

import onehajo.seurasaeng.entity.Answer;
import onehajo.seurasaeng.entity.Inquiry;
import onehajo.seurasaeng.entity.Manager;
import onehajo.seurasaeng.entity.User;
import onehajo.seurasaeng.inquiry.dto.AnswerReqDTO;
import onehajo.seurasaeng.inquiry.dto.InquiryDetailResDTO;
import onehajo.seurasaeng.inquiry.dto.InquiryReqDTO;
import onehajo.seurasaeng.inquiry.dto.InquiryResDTO;
import onehajo.seurasaeng.inquiry.exception.InquiryException;
import onehajo.seurasaeng.inquiry.exception.ManagerNotFoundException;
import onehajo.seurasaeng.inquiry.exception.UnauthorizedAccessException;
import onehajo.seurasaeng.inquiry.repository.AnswerRepository;
import onehajo.seurasaeng.inquiry.repository.InquiryRepository;
import onehajo.seurasaeng.inquiry.repository.ManagerRepository;
import onehajo.seurasaeng.inquiry.service.InquiryService;
import onehajo.seurasaeng.qr.exception.UserNotFoundException;
import onehajo.seurasaeng.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("문의 서비스 단위 테스트")
public class InquiryUnitTest {

    @Mock
    private InquiryRepository inquiryRepository;

    @Mock
    private AnswerRepository answerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ManagerRepository managerRepository;

    @InjectMocks
    private InquiryService inquiryService;

    private User testUser;
    private Manager testManager;
    private Inquiry testInquiry;
    private Answer testAnswer;
    private InquiryReqDTO inquiryReqDTO;
    private AnswerReqDTO answerReqDTO;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .name("테스트사용자")
                .email("test@example.com")
                .password("password123")
                .build();

        testManager = Manager.builder()
                .id(1L)
                .email("admin@example.com")
                .password("adminpassword")
                .build();

        testInquiry = Inquiry.builder()
                .id(1L)
                .user(testUser)
                .title("테스트 문의")
                .content("테스트 내용")
                .created_at(LocalDateTime.now())
                .answer_status(false)
                .build();

        testAnswer = Answer.builder()
                .id(1L)
                .inquiry(testInquiry)
                .manager(testManager)
                .answer("테스트 답변")
                .created_at(LocalDateTime.now())
                .build();

        inquiryReqDTO = InquiryReqDTO.builder()
                .title("새 문의")
                .content("새 문의 내용")
                .build();

        answerReqDTO = AnswerReqDTO.builder()
                .content("답변 내용")
                .build();
    }

    @Test
    @DisplayName("문의 작성 성공")
    void createInquiry_Success() {
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
        given(inquiryRepository.save(any(Inquiry.class))).willReturn(testInquiry);

        InquiryDetailResDTO result = inquiryService.createInquiry(1L, inquiryReqDTO);

        assertThat(result.getInquiry_id()).isEqualTo(1L);
        assertThat(result.getUser_name()).isEqualTo("테스트사용자");
        assertThat(result.getTitle()).isEqualTo("테스트 문의");
        assertThat(result.getContent()).isEqualTo("테스트 내용");
        assertThat(result.isAnswer_status()).isFalse();
        assertThat(result.getAnswer()).isNull();

        verify(userRepository, times(2)).findById(1L);
        verify(inquiryRepository).save(any(Inquiry.class));
    }

    @Test
    @DisplayName("문의 작성 실패 - 사용자 없음")
    void createInquiry_UserNotFound() {
        given(userRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> inquiryService.createInquiry(1L, inquiryReqDTO))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("사용자를 찾을 수 없습니다.");

        verify(userRepository).findById(1L);
        verify(inquiryRepository, never()).save(any(Inquiry.class));
    }

    @Test
    @DisplayName("사용자별 문의 목록 조회 성공")
    void getInquiriesByUserId_Success() {
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
        given(inquiryRepository.findByUserOrderByCreatedAtDesc(testUser)).willReturn(List.of(testInquiry));

        List<InquiryResDTO> result = inquiryService.getInquiriesByUserId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getTitle()).isEqualTo("테스트 문의");
        assertThat(result.get(0).isAnswer_status()).isFalse();

        verify(userRepository).findById(1L);
        verify(inquiryRepository).findByUserOrderByCreatedAtDesc(testUser);
    }

    @Test
    @DisplayName("사용자별 문의 목록 조회 실패 - 사용자 없음")
    void getInquiriesByUserId_UserNotFound() {
        given(userRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> inquiryService.getInquiriesByUserId(1L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("사용자를 찾을 수 없습니다.");

        verify(userRepository).findById(1L);
        verify(inquiryRepository, never()).findByUserOrderByCreatedAtDesc(any(User.class));
    }

    @Test
    @DisplayName("관리자 문의 목록 조회 성공")
    void getInquiriesByManagerId_Success() {
        given(managerRepository.findById(1L)).willReturn(Optional.of(testManager));
        given(inquiryRepository.findAll()).willReturn(List.of(testInquiry));

        List<InquiryResDTO> result = inquiryService.getInquiriesByManagerId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getTitle()).isEqualTo("테스트 문의");
        assertThat(result.get(0).isAnswer_status()).isFalse();

        verify(managerRepository).findById(1L);
        verify(inquiryRepository).findAll();
    }

    @Test
    @DisplayName("관리자 문의 목록 조회 실패 - 관리자 없음")
    void getInquiriesByManagerId_ManagerNotFound() {
        given(managerRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> inquiryService.getInquiriesByManagerId(1L))
                .isInstanceOf(ManagerNotFoundException.class)
                .hasMessage("관리자를 찾을 수 없습니다.");

        verify(managerRepository).findById(1L);
        verify(inquiryRepository, never()).findAll();
    }

    @Test
    @DisplayName("문의 상세 조회 성공 - 사용자")
    void getInquiryDetail_User_Success() {
        given(inquiryRepository.findById(1L)).willReturn(Optional.of(testInquiry));
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));

        InquiryDetailResDTO result = inquiryService.getInquiryDetail(1L, 1L);

        assertThat(result.getInquiry_id()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("테스트 문의");
        assertThat(result.getContent()).isEqualTo("테스트 내용");
        assertThat(result.isAnswer_status()).isFalse();
        assertThat(result.getAnswer()).isNull();

        verify(inquiryRepository).findById(1L);
        verify(userRepository).findById(1L);
    }

    @Test
    @DisplayName("문의 상세 조회 성공 - 관리자")
    void getAdminInquiryDetail_Success() {
        given(inquiryRepository.findById(1L)).willReturn(Optional.of(testInquiry));
        given(managerRepository.findById(1L)).willReturn(Optional.of(testManager));
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));

        InquiryDetailResDTO result = inquiryService.getAdminInquiryDetail(1L, 1L);

        assertThat(result.getInquiry_id()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("테스트 문의");
        assertThat(result.getContent()).isEqualTo("테스트 내용");

        verify(inquiryRepository).findById(1L);
        verify(managerRepository).findById(1L);
        verify(userRepository).findById(1L);
    }

    @Test
    @DisplayName("문의 상세 조회 실패 - 문의 없음")
    void getInquiryDetail_InquiryNotFound() {
        given(inquiryRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> inquiryService.getInquiryDetail(1L, 1L))
                .isInstanceOf(InquiryException.class)
                .hasMessage("문의를 찾을 수 없습니다.");

        verify(inquiryRepository).findById(1L);
    }

    @Test
    @DisplayName("문의 상세 조회 실패 - 권한 없음")
    void getInquiryDetail_UnauthorizedAccess() {
        User anotherUser = User.builder()
                .id(2L)
                .name("다른사용자")
                .email("another@example.com")
                .build();

        Inquiry anotherInquiry = Inquiry.builder()
                .id(1L)
                .user(anotherUser)
                .title("다른 사용자 문의")
                .content("접근 불가 내용")
                .created_at(LocalDateTime.now())
                .answer_status(false)
                .build();

        given(inquiryRepository.findById(1L)).willReturn(Optional.of(anotherInquiry));

        assertThatThrownBy(() -> inquiryService.getInquiryDetail(1L, 1L))
                .isInstanceOf(UnauthorizedAccessException.class)
                .hasMessage("본인의 문의만 접근할 수 있습니다.");

        verify(inquiryRepository).findById(1L);
    }

    @Test
    @DisplayName("문의 삭제 성공")
    void deleteInquiryById_Success() {
        given(inquiryRepository.findById(1L)).willReturn(Optional.of(testInquiry));

        inquiryService.deleteInquiryById(1L, 1L);

        verify(inquiryRepository).findById(1L);
        verify(inquiryRepository).deleteById(1L);
    }

    @Test
    @DisplayName("문의 삭제 실패 - 문의 없음")
    void deleteInquiryById_InquiryNotFound() {
        given(inquiryRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> inquiryService.deleteInquiryById(1L, 1L))
                .isInstanceOf(InquiryException.class)
                .hasMessage("문의를 찾을 수 없습니다.");

        verify(inquiryRepository).findById(1L);
        verify(inquiryRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("문의 삭제 실패 - 권한 없음")
    void deleteInquiryById_UnauthorizedAccess() {
        User anotherUser = User.builder()
                .id(2L)
                .name("다른사용자")
                .email("another@example.com")
                .build();

        Inquiry anotherInquiry = Inquiry.builder()
                .id(1L)
                .user(anotherUser)
                .title("다른 사용자 문의")
                .content("삭제 불가 내용")
                .created_at(LocalDateTime.now())
                .answer_status(false)
                .build();

        given(inquiryRepository.findById(1L)).willReturn(Optional.of(anotherInquiry));

        assertThatThrownBy(() -> inquiryService.deleteInquiryById(1L, 1L))
                .isInstanceOf(UnauthorizedAccessException.class)
                .hasMessage("본인의 문의만 삭제할 수 있습니다.");

        verify(inquiryRepository).findById(1L);
        verify(inquiryRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("문의 삭제 성공 - 답변 있는 경우")
    void deleteInquiryById_WithAnswer_Success() {
        Inquiry inquiryWithAnswer = Inquiry.builder()
                .id(1L)
                .user(testUser)
                .title("답변 있는 문의")
                .content("답변 있는 내용")
                .created_at(LocalDateTime.now())
                .answer_status(true)
                .build();

        given(inquiryRepository.findById(1L)).willReturn(Optional.of(inquiryWithAnswer));

        inquiryService.deleteInquiryById(1L, 1L);

        verify(inquiryRepository).findById(1L);
        verify(answerRepository).deleteByInquiryId(1L);
        verify(inquiryRepository).deleteById(1L);
    }

    @Test
    @DisplayName("답변 작성 성공")
    void saveAnswer_Success() {
        given(inquiryRepository.findById(1L)).willReturn(Optional.of(testInquiry));
        given(managerRepository.findById(1L)).willReturn(Optional.of(testManager));
        given(answerRepository.save(any(Answer.class))).willReturn(testAnswer);

        Inquiry updatedInquiry = Inquiry.builder()
                .id(1L)
                .user(testUser)
                .title("테스트 문의")
                .content("테스트 내용")
                .created_at(LocalDateTime.now())
                .answer_status(true)
                .build();

        given(inquiryRepository.findById(1L)).willReturn(Optional.of(updatedInquiry));
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));

        InquiryDetailResDTO result = inquiryService.saveAnswer(1L, 1L, answerReqDTO);

        assertThat(result.getInquiry_id()).isEqualTo(1L);
        assertThat(result.isAnswer_status()).isTrue();
        assertThat(result.getAnswer()).isNotNull();
        assertThat(result.getAnswer().getAnswer_content()).isEqualTo("테스트 답변");

        verify(inquiryRepository, times(2)).findById(1L);
        verify(managerRepository).findById(1L);
        verify(answerRepository).save(any(Answer.class));
        verify(inquiryRepository).updateAnswerStatus(1L, true);
    }

    @Test
    @DisplayName("답변 작성 실패 - 문의 없음")
    void saveAnswer_InquiryNotFound() {
        given(inquiryRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> inquiryService.saveAnswer(1L, 1L, answerReqDTO))
                .isInstanceOf(InquiryException.class)
                .hasMessage("문의를 찾을 수 없습니다.");

        verify(inquiryRepository).findById(1L);
        verify(answerRepository, never()).save(any(Answer.class));
    }

    @Test
    @DisplayName("답변 작성 실패 - 관리자 없음")
    void saveAnswer_ManagerNotFound() {
        given(inquiryRepository.findById(1L)).willReturn(Optional.of(testInquiry));
        given(managerRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> inquiryService.saveAnswer(1L, 1L, answerReqDTO))
                .isInstanceOf(ManagerNotFoundException.class)
                .hasMessage("관리자를 찾을 수 없습니다.");

        verify(inquiryRepository).findById(1L);
        verify(managerRepository).findById(1L);
        verify(answerRepository, never()).save(any(Answer.class));
    }

    @Test
    @DisplayName("답변이 있는 문의 상세 조회")
    void getInquiryDetail_WithAnswer_Success() {
        Inquiry inquiryWithAnswer = Inquiry.builder()
                .id(1L)
                .user(testUser)
                .title("답변 있는 문의")
                .content("답변 있는 내용")
                .created_at(LocalDateTime.now())
                .answer_status(true)
                .build();

        given(inquiryRepository.findById(1L)).willReturn(Optional.of(inquiryWithAnswer));
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
        given(answerRepository.findByInquiryId(1L)).willReturn(Optional.of(testAnswer));

        InquiryDetailResDTO result = inquiryService.getInquiryDetail(1L, 1L);

        assertThat(result.getInquiry_id()).isEqualTo(1L);
        assertThat(result.isAnswer_status()).isTrue();
        assertThat(result.getAnswer()).isNotNull();
        assertThat(result.getAnswer().getAnswer_content()).isEqualTo("테스트 답변");

        verify(inquiryRepository).findById(1L);
        verify(answerRepository).findByInquiryId(1L);
    }
}