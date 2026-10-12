package net.luversof.web.gate.poe;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.luversof.web.gate.poe.config.PoeDataVersion;

/** 아이콘 PNG 크기 → 인벤토리 칸(10-12 C280) — 게임 2DArt 는 칸당 52px + 2. PNG 가 아니거나 없거나 밖 경로면 null. */
class PoeDataVersionIconCellsTest {

  @TempDir Path dir;

  private Path png(String name, int w, int h) throws Exception {
    ByteBuffer b = ByteBuffer.allocate(24);
    b.put(
        new byte[] {
          (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n', 0, 0, 0, 13, 'I', 'H', 'D', 'R'
        });
    b.putInt(w).putInt(h);
    Path file = Files.createDirectories(dir.resolve("poe2/icons/uniques")).resolve(name);
    Files.write(file, b.array());
    return file;
  }

  @Test
  void 그림_크기에서_칸_수() throws Exception {
    png("body.png", 106, 158);
    png("ring.png", 54, 54);
    png("bow.png", 106, 210);
    new PoeDataVersion(dir.toString());
    assertThat(PoeDataVersion.iconCells("/poe-data/poe2/icons/uniques/body.png"))
        .containsExactly(2, 3);
    assertThat(PoeDataVersion.iconCells("/poe-data/poe2/icons/uniques/ring.png"))
        .containsExactly(1, 1);
    assertThat(PoeDataVersion.iconCells("/poe-data/poe2/icons/uniques/bow.png"))
        .containsExactly(2, 4);
  }

  @Test
  void 그림이_바뀌면_다시_읽는다() throws Exception {
    Path file = png("belt.png", 106, 54);
    new PoeDataVersion(dir.toString());
    assertThat(PoeDataVersion.iconCells("/poe-data/poe2/icons/uniques/belt.png"))
        .containsExactly(2, 1);
    png("belt.png", 54, 158);
    Files.setLastModifiedTime(
        file,
        java.nio.file.attribute.FileTime.fromMillis(
            Files.getLastModifiedTime(file).toMillis() + 5_000));
    assertThat(PoeDataVersion.iconCells("/poe-data/poe2/icons/uniques/belt.png"))
        .containsExactly(1, 3);
  }

  @Test
  void PNG_아님_없음_밖_경로는_null() throws Exception {
    Files.writeString(
        Files.createDirectories(dir.resolve("poe2")).resolve("note.png"),
        "not a png at all, just text");
    new PoeDataVersion(dir.toString());
    assertThat(PoeDataVersion.iconCells("/poe-data/poe2/note.png")).isNull();
    assertThat(PoeDataVersion.iconCells("/poe-data/poe2/none.png")).isNull();
    assertThat(PoeDataVersion.iconCells("/poe-data/../x.png")).isNull();
    assertThat(PoeDataVersion.iconCells("/poe-assets/a.png")).isNull();
    assertThat(PoeDataVersion.iconCells(null)).isNull();
  }
}
