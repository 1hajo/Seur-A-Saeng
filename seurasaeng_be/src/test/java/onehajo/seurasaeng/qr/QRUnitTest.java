package onehajo.seurasaeng.qr;

import com.fasterxml.jackson.databind.ObjectMapper;
import onehajo.seurasaeng.entity.*;
import onehajo.seurasaeng.qr.dto.QrReqDTO;
import onehajo.seurasaeng.qr.dto.ValidUserResDTO;
import onehajo.seurasaeng.qr.exception.InvalidQRCodeException;
import onehajo.seurasaeng.qr.exception.UserNotFoundException;
import onehajo.seurasaeng.qr.repository.QrRepository;
import onehajo.seurasaeng.qr.service.BoardingService;
import onehajo.seurasaeng.qr.service.QRService;
import onehajo.seurasaeng.qr.service.S3Service;
import onehajo.seurasaeng.qr.util.AESUtil;
import onehajo.seurasaeng.shuttle.repository.ShuttleRepository;
import onehajo.seurasaeng.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("QR 서비스 단위 테스트")
public class QRUnitTest {

    @Mock
    private AESUtil aesUtil;

    @Mock
    private S3Service s3Service;

    @Mock
    private BoardingService boardingService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private QrRepository qrRepository;

    @Mock
    private ShuttleRepository shuttleRepository;

    @InjectMocks
    private QRService qrService;

    private ObjectMapper objectMapper = new ObjectMapper();
    private User testUser;
    private Shuttle testShuttle;
    private Boarding testBoarding;
    private Qr testQr;

    @BeforeEach
    void setUp() {
        Location departure = Location.builder()
                .locationId(1L)
                .locationName("청사역")
                .latitude(36.6372)
                .longitude(127.2908)
                .build();

        Location destination = Location.builder()
                .locationId(2L)
                .locationName("아이티센")
                .latitude(36.6402)
                .longitude(127.2961)
                .build();

        testShuttle = Shuttle.builder()
                .id(1L)
                .shuttleName("청사역-아이티센 셔틀")
                .departure(departure)
                .destination(destination)
                .isCommute(true)
                .build();

        testUser = User.builder()
                .id(1L)
                .name("테스트사용자")
                .email("test@example.com")
                .password("password123")
                .build();

        testBoarding = Boarding.builder()
                .id(1L)
                .user_id(testUser.getId())
                .shuttle(testShuttle)
                .boarding_time(LocalDateTime.now())
                .build();

        testQr = Qr.builder()
                .id(1L)
                .user(testUser)
                .qrCode("https://s3.amazonaws.com/test-bucket/qr-code.png")
                .build();
    }

    @Test
    @DisplayName("QR 코드 생성 성공")
    void generateQRCodeSuccess() throws Exception {
        // Given
        String encryptedData = "encrypted_qr_data";
        String s3Url = "https://s3.amazonaws.com/test-bucket/qr-code.png";

        given(userRepository.findById(testUser.getId()))
                .willReturn(Optional.of(testUser));
        given(aesUtil.encrypt(anyString()))
                .willReturn(encryptedData);
        given(s3Service.uploadQRToS3(any(byte[].class)))
                .willReturn(s3Url);
        given(qrRepository.save(any(Qr.class)))
                .willReturn(testQr);

        // When
        qrService.generateQRCode(testUser.getId(), testUser.getEmail());

        // Then
        verify(userRepository).findById(testUser.getId());
        verify(aesUtil).encrypt(anyString());
        verify(s3Service).uploadQRToS3(any(byte[].class));
        verify(qrRepository).save(any(Qr.class));
    }

    @Test
    @DisplayName("존재하지 않는 사용자 QR 생성 실패")
    void generateQRCodeFailWithNonExistentUser() {
        // Given
        given(userRepository.findById(testUser.getId()))
                .willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() ->
                qrService.generateQRCode(testUser.getId(), testUser.getEmail()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User not found");
    }

    @Test
    @DisplayName("사용자 ID로 QR 코드 조회 성공")
    void getQRCodeByUserIdSuccess() {
        // Given
        byte[] imageBytes = new byte[]{1, 2, 3, 4, 5};
        given(qrRepository.findByUserId(testUser.getId()))
                .willReturn(Optional.of(testQr));
        given(s3Service.downloadFileFromS3(testQr.getQrCode()))
                .willReturn(imageBytes);

        // When
        String result = qrService.getQRCodeByUserId(testUser.getId());

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
    }

    @Test
    @DisplayName("QR 코드 검증 및 탑승 처리 성공")
    void validateQRCodeAndBoardingSuccess() throws Exception {
        // Given
        String qrCode = "encrypted_qr_code";
        QrReqDTO qrRequest = new QrReqDTO(testUser.getId(), testUser.getEmail());
        String decryptedString = objectMapper.writeValueAsString(qrRequest);

        given(aesUtil.decrypt(qrCode))
                .willReturn(decryptedString);
        given(userRepository.findByIdAndEmail(testUser.getId(), testUser.getEmail()))
                .willReturn(Optional.of(testUser));
        given(shuttleRepository.findById(testShuttle.getId()))
                .willReturn(Optional.of(testShuttle));
        given(boardingService.saveBoardingRecord(testUser.getId(), testShuttle.getId()))
                .willReturn(testBoarding);
        given(boardingService.incrementBoardingCount(testShuttle.getId()))
                .willReturn(1L);

        // When
        ValidUserResDTO result = qrService.userValidate(qrCode, testShuttle.getId());

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUser_name()).isEqualTo(testUser.getName());
        assertThat(result.getDeparture()).isEqualTo("청사역");
        assertThat(result.getDestination()).isEqualTo("아이티센");
    }

    @Test
    @DisplayName("잘못된 QR 코드 검증 실패")
    void validateInvalidQRCodeFail() throws Exception {
        // Given
        String invalidQrCode = "invalid_qr_code";
        given(aesUtil.decrypt(invalidQrCode))
                .willThrow(new Exception("Decryption failed"));

        // When & Then
        assertThatThrownBy(() ->
                qrService.userValidate(invalidQrCode, testShuttle.getId()))
                .isInstanceOf(InvalidQRCodeException.class)
                .hasMessage("유효하지 않은 QR 코드입니다.");
    }

    @Test
    @DisplayName("존재하지 않는 사용자 검증 실패")
    void validateNonExistentUserFail() throws Exception {
        // Given
        String qrCode = "encrypted_qr_code";
        QrReqDTO qrRequest = new QrReqDTO(testUser.getId(), testUser.getEmail());
        String decryptedString = objectMapper.writeValueAsString(qrRequest);

        given(aesUtil.decrypt(qrCode))
                .willReturn(decryptedString);
        given(userRepository.findByIdAndEmail(testUser.getId(), testUser.getEmail()))
                .willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() ->
                qrService.userValidate(qrCode, testShuttle.getId()))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("유효하지 않은 사용자입니다.");
    }

    @Test
    @DisplayName("존재하지 않는 셔틀 검증 실패")
    void validateNonExistentShuttleFail() throws Exception {
        // Given
        String qrCode = "encrypted_qr_code";
        QrReqDTO qrRequest = new QrReqDTO(testUser.getId(), testUser.getEmail());
        String decryptedString = objectMapper.writeValueAsString(qrRequest);

        given(aesUtil.decrypt(qrCode))
                .willReturn(decryptedString);
        given(userRepository.findByIdAndEmail(testUser.getId(), testUser.getEmail()))
                .willReturn(Optional.of(testUser));
        given(shuttleRepository.findById(testShuttle.getId()))
                .willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() ->
                qrService.userValidate(qrCode, testShuttle.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 셔틀입니다. Shuttle ID: " + testShuttle.getId());
    }

    @Test
    @DisplayName("중복 탑승으로 검증 실패")
    void validateDuplicateBoardingFail() throws Exception {
        // Given
        String qrCode = "encrypted_qr_code";
        QrReqDTO qrRequest = new QrReqDTO(testUser.getId(), testUser.getEmail());
        String decryptedString = objectMapper.writeValueAsString(qrRequest);

        given(aesUtil.decrypt(qrCode))
                .willReturn(decryptedString);
        given(userRepository.findByIdAndEmail(testUser.getId(), testUser.getEmail()))
                .willReturn(Optional.of(testUser));
        given(shuttleRepository.findById(testShuttle.getId()))
                .willReturn(Optional.of(testShuttle));
        willThrow(new IllegalArgumentException("오늘 이미 해당 셔틀에 탑승하셨습니다."))
                .given(boardingService).checkDuplicateBoarding(testUser.getId(), testShuttle.getId());

        // When & Then
        assertThatThrownBy(() ->
                qrService.userValidate(qrCode, testShuttle.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("오늘 이미 해당 셔틀에 탑승하셨습니다.");
    }
}