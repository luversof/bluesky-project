package net.luversof;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;


@SpringBootTest(classes = TestApplication.class, webEnvironment = WebEnvironment.NONE)
public interface GeneralTest {
}
