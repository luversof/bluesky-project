package net.luversof.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleTest {

	private static final Logger log = LoggerFactory.getLogger(SimpleTest.class);

	@Test
	public void test() {

		int[][] arr = Arrays.array(
				new int[] { 9, 20, 28, 18, 11 },
				new int[] { 30, 1, 21, 17, 28 });

		for (int i = 0; i < arr[0].length; i++) {
			log.debug("result : {}", Integer.toBinaryString(arr[0][i] | arr[1][i]).replace("1", "#").replace("0", " "));
		}

		assertThat(arr).isNotEmpty();
	}
}
