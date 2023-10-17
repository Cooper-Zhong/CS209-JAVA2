package lab5;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;

public class BufferReader {

	public static void main(String[] args) throws URISyntaxException {
		String filePath = "/Users/cooperz/SUSTech/2023_Fall/CS209_Java2/CS209-JAVA2-repo/Java2-lab/src/main/java/lab5/gb2312.txt";

		try (FileInputStream fis = new FileInputStream(filePath);
			 InputStreamReader isr = new InputStreamReader(fis, "gb2312");
			 BufferedReader bReader = new BufferedReader(isr);){

			char[] cbuf = new char[200];
			int file_len = bReader.read(cbuf);

			System.out.println(file_len);
			System.out.println(cbuf);
			
		} catch (FileNotFoundException e) {
			System.out.println("The pathname does not exist.");
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			System.out.println("The Character Encoding is not supported.");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("Failed or interrupted when doing the I/O operations");
			e.printStackTrace();
		}
	}
}
