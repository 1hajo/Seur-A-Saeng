package onehajo.seurasaeng;

import onehajo.seurasaeng.user.dto.SignUpReqDTO;
import onehajo.seurasaeng.user.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

@SpringBootApplication
public class SeurasaengApplication {
    public static void main(String[] args) {
        SpringApplication.run(SeurasaengApplication.class, args);
    }

    @Component
    class AdminUserInitializer implements CommandLineRunner {

        private final UserService userService;

        public AdminUserInitializer(UserService userService) {
            this.userService = userService;
        }

        @Override
        public void run(String... args) throws Exception {
            SignUpReqDTO req1 = SignUpReqDTO.builder()
                    .email("youjiyeon416@gmail.com")
                    .password("seurasaeng0428")
                    .role("admin")
                    .build();

            SignUpReqDTO req2 = SignUpReqDTO.builder()
                    .email("tm003512@gmail.com")
                    .password("seurasaeng0428")
                    .role("admin")
                    .build();

            SignUpReqDTO req3 = SignUpReqDTO.builder()
                    .email("yujinkimvv@gmail.com")
                    .password("seurasaeng0428")
                    .role("admin")
                    .build();

            SignUpReqDTO req4 = SignUpReqDTO.builder()
                    .email("itcenljj@gmail.com")
                    .password("seurasaeng0428")
                    .role("admin")
                    .build();

            SignUpReqDTO req5 = SignUpReqDTO.builder()
                    .email("tn25800@gmail.com")
                    .password("seurasaeng0428")
                    .role("admin")
                    .build();

            SignUpReqDTO req6 = SignUpReqDTO.builder()
                    .email("dhfkdlsj1@gmail.com")
                    .password("seurasaeng0428")
                    .role("admin")
                    .build();

            SignUpReqDTO req7 = SignUpReqDTO.builder()
                    .email("lbm000314@gmail.com")
                    .password("seurasaeng0428")
                    .role("admin")
                    .build();

            SignUpReqDTO req8 = SignUpReqDTO.builder()
                    .email("hdmksm1@gmail.com")
                    .password("seurasaeng0428")
                    .role("admin")
                    .build();

            SignUpReqDTO req9 = SignUpReqDTO.builder()
                    .email("oqsis55@gmail.com")
                    .password("seurasaeng0428")
                    .role("admin")
                    .build();

            SignUpReqDTO req10 = SignUpReqDTO.builder()
                    .email("itcenceni@gmail.com")
                    .password("seurasaeng0428")
                    .role("admin")
                    .build();

            userService.registerAdmin(req1);
            userService.registerAdmin(req2);
            userService.registerAdmin(req3);
            userService.registerAdmin(req4);
            userService.registerAdmin(req5);
            userService.registerAdmin(req6);
            userService.registerAdmin(req7);
            userService.registerAdmin(req8);
            userService.registerAdmin(req9);
            userService.registerAdmin(req10);
            System.out.println("관리자 계정이 초기화되었습니다.");
        }
    }
}
