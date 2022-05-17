package de.dlr.proseo.storagemgr.version2.s3;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import de.dlr.proseo.storagemgr.StorageManager;
import de.dlr.proseo.storagemgr.StorageManagerConfiguration;
import de.dlr.proseo.storagemgr.StorageTestUtils;
import de.dlr.proseo.storagemgr.TestUtils;
import de.dlr.proseo.storagemgr.version2.posix.PosixDAL;
import de.dlr.proseo.storagemgr.version2.s3.S3DAL;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.transfer.s3.FileUpload;
import software.amazon.awssdk.transfer.s3.S3ClientConfiguration;
import software.amazon.awssdk.transfer.s3.S3TransferManager;

/**
 * @author Denys Chaykovskiy
 *
 */

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = StorageManager.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestEntityManager
public class S3DALTest {

	@Autowired
	private TestUtils testUtils;

	@Autowired
	private StorageManagerConfiguration cfg;
	
	@Autowired
	private StorageTestUtils storageTestUtils;

	@Rule
	public TestName testName = new TestName();

	String testCachePath;
	String cachePath;
	String testSourcePath;

	@PostConstruct
	private void init() {
		testCachePath = testUtils.getTestCachePath();
		cachePath = testUtils.getCachePath();

		testSourcePath = testUtils.getTestSourcePath();
	}

	@Test
	public void testS3_uploadDownload() throws Exception {

		TestUtils.printMethodName(this, testName);
		TestUtils.createEmptyStorageDirectories();

		String prefix = "files/";

		List<String> pathes = new ArrayList<>();
		pathes.add(prefix + "file1.txt");
		pathes.add(prefix + "file2.txt");
		pathes.add(prefix + "dir/file3.txt");

		List<String> sourcePathes = new ArrayList<>();

		for (String path : pathes) {

			String sourcePath = storageTestUtils.createSourceFile(path);
			sourcePathes.add(sourcePath);
		}

		String sourcePath = testUtils.getSourcePath();
		String storagePath = testUtils.getStoragePath();

		S3DAL s3DAL = new S3DAL( cfg.getS3AccessKey(), cfg.getS3SecretAccessKey(), cfg.getS3DefaultBucket());
		
		s3DAL.deleteFiles();

		try {
			// create source files
			List<String> sourceFiles = s3DAL.getFiles(sourcePath);
			TestUtils.printList("Source Files: ", sourceFiles);
			//assertTrue("Expected: 3, " + " Exists: " + sourceFiles.size(), sourceFiles.size() == 3);

			// upload files to storage
			List<String> uploadedFiles = s3DAL.upload(sourcePath, storagePath);
			TestUtils.printList("Uploaded Files: ", uploadedFiles);
			assertTrue("Expected: 3, " + " Exists: " + uploadedFiles.size(), uploadedFiles.size() == 3);
			TestUtils.printList("S3 Storage after upload: ", s3DAL.getFiles());
			
			// delete source files
			//List<String> deletedFiles = s3DAL.delete(sourcePath);
			//TestUtils.printList("Deleted Files: ", deletedFiles);
			//assertTrue("Expected: 3, " + " Exists: " + deletedFiles.size(), deletedFiles.size() == 3);

			// download files from storage
			List<String> downloadedFiles = s3DAL.download(storagePath, sourcePath);
			TestUtils.printList("Downloaded Files: ", downloadedFiles);
			assertTrue("Expected: 3, " + " Exists: " + downloadedFiles.size(), downloadedFiles.size() == 3);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
