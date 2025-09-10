package lk.swiftlogistics.wms.web;

import lk.swiftlogistics.wms.config.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody Map<String, String> body) {
        // ⚠️ For demo only: accept any username
        String username = body.get("username");
        String token = jwtUtil.generateToken(username);
        return Map.of("token", token);
    }
}
