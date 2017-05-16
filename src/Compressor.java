import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;


public class Compressor {
  public static void compress(String inputFilePath, String outputFilePath) throws IOException {
    FileInputStream file = new FileInputStream(inputFilePath);
    int avail = file.available();
    byte[] bytes = new byte[avail];
    file.read(bytes);
    BinFile binFile = new BinFile(outputFilePath);
    int i = 0;
    while (i < bytes.length) {
      boolean appended = false;
      for (int window = (1 << 4); window >= 2; window--) {
        if (appended) {
          break;
        }
        int start = Math.max(0, i - ((1 << 12) - 1));
        for (int index = start; index <= i - window; index++) {
          if (appended) {
            break;
          }
          if (matches(bytes, i, i + window - 1, index, index + window - 1)) {
            binFile.appendBits(1, 1);
            binFile.appendBits(i - index - 1, 12);
            binFile.appendBits(window - 1, 4);
            i += window;
            appended = true;
          }
        }
      }
      if (!appended) {
        binFile.appendBits(0, 1);
        binFile.appendBits(bytes[i], 8);
        i++;
      }
    }
    binFile.close();
    file.close();
  }

  private static boolean matches(byte[] bytes, int start1, int end1, int start2, int end2) {
    if (start1 - end1 != start2 - end2) {
      return false;
    }
    if (start1 < 0 || start2 < 0 || end1 >= bytes.length || end2 >= bytes.length) {
      return false;
    }
    for (int i = 0; i <= end1 - start1; i++) {
      if (bytes[start1 + i] != bytes[start2 + i]) {
        return false;
      }
    }
    return true;
  }

  public static void decompress(String inputFilePath, String outputFilePath) throws IOException {
    FileInputStream file = new FileInputStream(inputFilePath);

    int avail = file.available();
    byte[] bytes = new byte[avail];
    file.read(bytes);
    CompressedData compressedData = new CompressedData(bytes);
    FileOutputStream outFile = new FileOutputStream(outputFilePath);
    while ((bytes = compressedData.readNextBytes()) != null) {
      outFile.write(bytes);
    }
    outFile.close();
  }

  static class CompressedData {
    private final byte[] _bytes;
    private int _bitOffset = 0;
    private List<Byte> _outputBytes;

    public CompressedData(byte[] bytes) {
      _bytes = bytes;
      _outputBytes = new ArrayList<>();
    }

    private byte[] readNextBytes() {
      Integer op = readBit();
      if (op == null) {
        return null;
      }
      if (op == 0) {
        Long nextByte = readBits(8);
        if (nextByte == null) {
          return null;
        }
        return saveAndReturn(new byte[]{(byte) (nextByte * 1L)});
      } else {
        Long offset = readBits(12);
        Long length = readBits(4);
        int start = (int) (_outputBytes.size() - offset - 1);
        List<Byte> nextBytes = _outputBytes.subList(start, (int) (start + length + 1));
        byte[] bytes = new byte[nextBytes.size()];
        for (int i = 0; i < bytes.length; i++) {
          bytes[i] = nextBytes.get(i);
        }
        return saveAndReturn(bytes);
      }
    }

    private byte[] saveAndReturn(byte[] bytes) {
      for (byte b : bytes) {
        _outputBytes.add(b);
      }
      return bytes;
    }

    private Long readBits(int length) {
      long b = 0;
      for (int i = 0; i < length; i++) {
        Integer bit = readBit();
        if (bit == null) {
          return null;
        }
        b = (b << 1) + bit;
      }
      return b;
    }

    private Integer readBit() {
      if ((_bitOffset / 8) >= _bytes.length) {
        return null;
      }
      byte currentByte = _bytes[(int) (_bitOffset / 8)];
      int bitPos = _bitOffset % 8;
      _bitOffset++;
      return (currentByte & (1 << (7 - bitPos))) >> (7 - bitPos);
    }
  }

  static class BinFile {
    private OutputStream _outputStream;
    byte _currentByte;
    int _currentByteLen = 0;

    public BinFile(String fileName) throws FileNotFoundException {
      _outputStream = new FileOutputStream(fileName);
    }

    private void appendBits(int bits, int bitLen) throws IOException {
      if (bitLen == 0) {
        return;
      }
      int fit = Math.min(bitLen, 8 - _currentByteLen);
      int shave = bitLen - fit;
      _currentByte = (byte) (_currentByte << fit);
      _currentByte += bits >> shave;
      _currentByteLen += fit;
      if (_currentByteLen == 8) {
        _outputStream.write(new byte[]{_currentByte});
        _currentByte = 0;
        _currentByteLen = 0;
        if (shave > 0) {
          int rem = bits & ((1 << shave) - 1);
          appendBits(rem, shave);
        }
      }
    }

    public void close() throws IOException {
      if (_currentByteLen > 0) {
        _outputStream.write(new byte[]{(byte) (_currentByte << (8 - _currentByteLen))});
        _currentByte = 0;
        _currentByteLen = 0;
      }
      _outputStream.close();
    }
  }

  public static void main(String[] args) throws IOException {
    System.out.println(Integer.toBinaryString(97));
    Compressor.compress("test.bin", "compressed.bin");
    Compressor.decompress("compressed.bin", "test2.bin");
  }
}
