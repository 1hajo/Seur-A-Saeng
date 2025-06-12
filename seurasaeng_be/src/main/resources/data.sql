-- 새로운 Location 삽입
INSERT INTO seurasaeng_test.location (location_id, location_name, latitude, longitude) VALUES
(1, '정부과천청사역', 37.4254, 126.9892),
(2, '양재역', 37.4837, 127.0354),
(3, '사당역', 37.4754, 126.9814),
(4, '이수역', 37.4854, 126.9821),
(5, '금정역', 37.3716, 126.9435),
(6, '아이티센타워', 37.4173, 126.9912);

-- 새로운 Shuttle 삽입
INSERT INTO seurasaeng_test.shuttle (shuttle_id, shuttle_name, departure, destination, is_commute) VALUES
(1, '정부과천청사', 1, 6, true),
(2, '양재', 2, 6, true),
(3, '사당', 3, 6, true),
(4, '이수', 4, 6, true),
(5, '금정', 5, 6, true),
(6, '정부과천청사', 6, 1, false),
(7, '양재', 6, 2, false),
(8, '사당', 6, 3, false),
(9, '이수', 6, 4, false),
(10, '금정', 6, 5, false);


-- 출근 셔틀 1 (정부과천청사역 -> 아이티센타워) 시간표
INSERT INTO seurasaeng_test.timetable (timetable_id, shuttle_id, departure_time, boarding_location, dropoff_location, arrival_minutes, total_seats) VALUES
(1, 1, '07:20', '7번출구 앞', 'G동 옆 도로', 15, 45),
(2, 1, '07:40', '7번출구 앞', 'G동 옆 도로', 15, 45),
(3, 1, '08:00', '7번출구 앞', 'G동 옆 도로', 15, 45),
(4, 1, '08:20', '7번출구 앞', 'G동 옆 도로', 15, 45),
(5, 1, '08:40', '7번출구 앞', 'G동 옆 도로', 15, 45),
(6, 1, '09:00', '7번출구 앞', 'G동 옆 도로', 15, 45),
(7, 1, '09:20', '7번출구 앞', 'G동 옆 도로', 15, 45),
(8, 1, '09:40', '7번출구 앞', 'G동 옆 도로', 15, 45);

-- 출근 셔틀 2 (양재역 -> 아이티센타워) 시간표
INSERT INTO seurasaeng_test.timetable (timetable_id, shuttle_id, departure_time, boarding_location, dropoff_location, arrival_minutes, total_seats) VALUES
(9, 2, '07:00', '9번출구 서초문화예술회관 앞', 'G동 옆 도로', 20, 45),
(10, 2, '07:20', '9번출구 서초문화예술회관 앞', 'G동 옆 도로', 20, 45),
(11, 2, '08:10', '9번출구 서초문화예술회관 앞', 'G동 옆 도로', 20, 45),
(12, 2, '08:30', '9번출구 서초문화예술회관 앞', 'G동 옆 도로', 20, 45),
(13, 2, '09:20', '9번출구 서초문화예술회관 앞', 'G동 옆 도로', 20, 45),
(14, 2, '09:40', '9번출구 서초문화예술회관 앞', 'G동 옆 도로', 20, 45);

-- 출근 셔틀 3 (사당역 -> 아이티센타워) 시간표
INSERT INTO seurasaeng_test.timetable (timetable_id, shuttle_id, departure_time, boarding_location, dropoff_location, arrival_minutes, total_seats) VALUES
(15, 3, '07:00', '4번출구 티스테이션 앞', 'G동 옆 도로', 20, 45),
(16, 3, '07:30', '4번출구 티스테이션 앞', 'G동 옆 도로', 20, 45),
(17, 3, '08:00', '4번출구 티스테이션 앞', 'G동 옆 도로', 20, 45),
(18, 3, '08:30', '4번출구 티스테이션 앞', 'G동 옆 도로', 20, 45),
(19, 3, '09:00', '4번출구 티스테이션 앞', 'G동 옆 도로', 20, 45),
(20, 3, '09:30', '4번출구 티스테이션 앞', 'G동 옆 도로', 20, 45);

-- 출근 셔틀 4 (이수역 -> 아이티센타워) 시간표
INSERT INTO seurasaeng_test.timetable (timetable_id, shuttle_id, departure_time, boarding_location, dropoff_location, arrival_minutes, total_seats) VALUES
(21, 4, '07:00', '7번출구 택시정류장', 'G동 옆 도로', 30, 45),
(22, 4, '07:30', '7번출구 택시정류장', 'G동 옆 도로', 30, 45),
(23, 4, '08:10', '7번출구 택시정류장', 'G동 옆 도로', 30, 45),
(24, 4, '08:40', '7번출구 택시정류장', 'G동 옆 도로', 30, 45),
(25, 4, '09:20', '7번출구 택시정류장', 'G동 옆 도로', 30, 45);

-- 출근 셔틀 5 (금정역 -> 아이티센타워) 시간표
INSERT INTO seurasaeng_test.timetable (timetable_id, shuttle_id, departure_time, boarding_location, dropoff_location, arrival_minutes, total_seats) VALUES
(26, 5, '07:00', '3번출구 버스정류장', 'G동 옆 도로', 30, 45),
(27, 5, '08:10', '3번출구 버스정류장', 'G동 옆 도로', 30, 45),
(28, 5, '09:20', '3번출구 버스정류장', 'G동 옆 도로', 30, 45);

-- 퇴근 셔틀 6 (아이티센타워 -> 정부과천청사역) 시간표
INSERT INTO seurasaeng_test.timetable (timetable_id, shuttle_id, departure_time, boarding_location, dropoff_location, arrival_minutes, total_seats) VALUES
(29, 6, '17:10', 'G동 옆 도로', '8번출구 앞', 15, 45),
(30, 6, '17:40', 'G동 옆 도로', '8번출구 앞', 15, 45),
(31, 6, '18:10', 'G동 옆 도로', '8번출구 앞', 15, 45),
(32, 6, '18:40', 'G동 옆 도로', '8번출구 앞', 15, 45),
(33, 6, '19:10', 'G동 옆 도로', '8번출구 앞', 15, 45),
(34, 6, '19:40', 'G동 옆 도로', '8번출구 앞', 15, 45);

-- 퇴근 셔틀 7 (아이티센타워 -> 양재역) 시간표
INSERT INTO seurasaeng_test.timetable (timetable_id, shuttle_id, departure_time, boarding_location, dropoff_location, arrival_minutes, total_seats) VALUES
(35, 7, '17:10', 'G동 옆 도로', '8번,9번출구 중앙버스정류장', 20, 45),
(36, 7, '18:10', 'G동 옆 도로', '8번,9번출구 중앙버스정류장', 20, 45),
(37, 7, '19:10', 'G동 옆 도로', '8번,9번출구 중앙버스정류장', 20, 45);

-- 퇴근 셔틀 8 (아이티센타워 -> 사당역) 시간표
INSERT INTO seurasaeng_test.timetable (timetable_id, shuttle_id, departure_time, boarding_location, dropoff_location, arrival_minutes, total_seats) VALUES
(38, 8, '17:20', 'G동 옆 도로', '3번,4번출구 중앙버스정류장', 20, 45),
(39, 8, '18:20', 'G동 옆 도로', '3번,4번출구 중앙버스정류장', 20, 45),
(40, 8, '19:20', 'G동 옆 도로', '3번,4번출구 중앙버스정류장', 20, 45);

-- 퇴근 셔틀 9 (아이티센타워 -> 이수역) 시간표
INSERT INTO seurasaeng_test.timetable (timetable_id, shuttle_id, departure_time, boarding_location, dropoff_location, arrival_minutes, total_seats) VALUES
(41, 9, '17:10', 'G동 옆 도로', '6번,7번출구 중앙버스정류장', 30, 45),
(42, 9, '18:10', 'G동 옆 도로', '6번,7번출구 중앙버스정류장', 30, 45),
(43, 9, '19:10', 'G동 옆 도로', '6번,7번출구 중앙버스정류장', 30, 45);

-- 퇴근 셔틀 10 (아이티센타워 -> 금정역) 시간표
INSERT INTO seurasaeng_test.timetable (timetable_id, shuttle_id, departure_time, boarding_location, dropoff_location, arrival_minutes, total_seats) VALUES
(44, 10, '17:10', 'G동 옆 도로', '3번출구 앞', 30, 45),
(45, 10, '18:10', 'G동 옆 도로', '3번출구 앞', 30, 45),
(46, 10, '19:20', 'G동 옆 도로', '3번출구 앞', 30, 45);