package onehajo.seurasaeng.qr;

import onehajo.seurasaeng.entity.*;
import onehajo.seurasaeng.qr.dto.BoardingRecordResDTO;
import onehajo.seurasaeng.qr.exception.DuplicateBoardingException;
import onehajo.seurasaeng.qr.repository.BoardingRepository;
import onehajo.seurasaeng.qr.service.BoardingService;
import onehajo.seurasaeng.shuttle.exception.ShuttleNotFoundException;
import onehajo.seurasaeng.shuttle.repository.ShuttleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("탑승 서비스 단위 테스트")
public class BoardingUnitTest {

    @Mock
    private BoardingRepository boardingRepository;

    @Mock
    private ShuttleRepository shuttleRepository;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private BoardingService boardingService;

    private User testUser;
    private Shuttle testShuttle;
    private Boarding testBoarding;

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
    }

    @Test
    @DisplayName("탑승 내역 저장 성공")
    void saveBoardingRecordSuccess() {
        given(shuttleRepository.findById(testShuttle.getId()))
                .willReturn(Optional.of(testShuttle));
        given(boardingRepository.save(any(Boarding.class)))
                .willReturn(testBoarding);

        Boarding result = boardingService.saveBoardingRecord(testUser.getId(), testShuttle.getId());

        assertThat(result).isNotNull();
        assertThat(result.getUser_id()).isEqualTo(testUser.getId());
        assertThat(result.getShuttle().getId()).isEqualTo(testShuttle.getId());
    }

    @Test
    @DisplayName("존재하지 않는 셔틀로 탑승 내역 저장 실패")
    void saveBoardingRecordFailWithNonExistentShuttle() {
        given(shuttleRepository.findById(testShuttle.getId()))
                .willReturn(Optional.empty());

        assertThatThrownBy(() ->
                boardingService.saveBoardingRecord(testUser.getId(), testShuttle.getId()))
                .isInstanceOf(ShuttleNotFoundException.class)
                .hasMessage("해당 셔틀 노선을 찾을 수 없습니다. ID=" + testShuttle.getId());
    }

    @Test
    @DisplayName("사용자 탑승 내역 조회 성공")
    void getUserBoardingRecordSuccess() {
        List<Boarding> boardingList = Arrays.asList(testBoarding);
        given(boardingRepository.findByUser_idOrderByBoarding_timeDesc(testUser.getId()))
                .willReturn(boardingList);
        given(shuttleRepository.findById(testShuttle.getId()))
                .willReturn(Optional.of(testShuttle));

        List<BoardingRecordResDTO> result = boardingService.getUserBoardingRecord(testUser.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBoarding_id()).isEqualTo(testBoarding.getId());
        assertThat(result.get(0).getDeparture()).isEqualTo("청사역");
        assertThat(result.get(0).getDestination()).isEqualTo("아이티센");
    }

    @Test
    @DisplayName("Redis 탑승 인원 조회 성공")
    void getCurrentBoardingCountSuccess() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        String expectedKey = "boarding:count:shuttle:" + testShuttle.getId();
        given(valueOperations.get(expectedKey)).willReturn("5");

        Long result = boardingService.getCurrentBoardingCount(testShuttle.getId());

        assertThat(result).isEqualTo(5L);
    }

    @Test
    @DisplayName("Redis 탑승 인원 데이터 없을 때 0 반환")
    void getCurrentBoardingCountWhenNoData() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        String expectedKey = "boarding:count:shuttle:" + testShuttle.getId();
        given(valueOperations.get(expectedKey)).willReturn(null);

        Long result = boardingService.getCurrentBoardingCount(testShuttle.getId());

        assertThat(result).isEqualTo(0L);
    }

    @Test
    @DisplayName("Redis 탑승 인원 증가 성공")
    void incrementBoardingCountSuccess() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        String expectedKey = "boarding:count:shuttle:" + testShuttle.getId();
        given(valueOperations.increment(expectedKey)).willReturn(3L);

        Long result = boardingService.incrementBoardingCount(testShuttle.getId());

        assertThat(result).isEqualTo(3L);
    }

    @Test
    @DisplayName("중복 탑승 체크 - 중복 없음")
    void checkDuplicateBoardingNoDuplicate() {
        given(boardingRepository.existsByUser_idAndShuttleIdAndBoarding_timeBetween(
                eq(testUser.getId()), eq(testShuttle.getId()),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .willReturn(false);

        assertThatCode(() ->
                boardingService.checkDuplicateBoarding(testUser.getId(), testShuttle.getId()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("중복 탑승 체크 - 중복 발견")
    void checkDuplicateBoardingFound() {
        given(boardingRepository.existsByUser_idAndShuttleIdAndBoarding_timeBetween(
                eq(testUser.getId()), eq(testShuttle.getId()),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .willReturn(true);

        assertThatThrownBy(() ->
                boardingService.checkDuplicateBoarding(testUser.getId(), testShuttle.getId()))
                .isInstanceOf(DuplicateBoardingException.class)
                .hasMessage("오늘 이미 해당 셔틀에 탑승하셨습니다.");
    }
}