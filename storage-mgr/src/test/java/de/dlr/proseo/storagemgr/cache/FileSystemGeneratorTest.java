package de.dlr.proseo.storagemgr.cache;

/**
 * @author Denys Chaykovskiy
 *
 */
public class FileSystemGeneratorTest {

	private int maxDirectoryDepth = 3;
	private int filesToGenerate = 3;
	private int fileContentLength = 8;

	private String path;
	private PathGeneratorTest pathGenerator = new PathGeneratorTest();

	public static void main(String[] args) {

		String testPath = TestUtils.getTestPath();

		System.out.println("Current Path: " + testPath);

		var fileTreeGenerator = new FileSystemGeneratorTest(testPath);

		fileTreeGenerator.generate();
	}

	/**
	 * @return the maxDepth
	 */
	public int getMaxDepth() {

		return maxDirectoryDepth;
	}

	/**
	 * @param maxDepth the maxDepth to set
	 */
	public void setMaxDepth(int maxDepth) {

		this.maxDirectoryDepth = maxDepth;
	}

	/**
	 * @return the filesAmount
	 */
	public int getFilesAmount() {

		return filesToGenerate;
	}

	/**
	 * @param filesAmount the filesAmount to set
	 */
	public void setFilesAmount(int filesAmount) {

		this.filesToGenerate = filesAmount;
	}

	/**
	 * @return the path
	 */
	public String getPath() {

		return path;
	}

	/**
	 * @param path the path to set
	 */
	public void setPath(String path) {

		this.path = path;
	}

	/**
	 * @param path
	 */
	public FileSystemGeneratorTest(String path) {

		this.path = path;
	}

	/**
	 * @param maxDepth
	 * @param filesAmount
	 * @param path
	 */
	public FileSystemGeneratorTest(int maxDepth, int filesAmount, String path) {

		this(path);

		this.maxDirectoryDepth = maxDepth;
		this.filesToGenerate = filesAmount;
	}

	/**
	 * 
	 */
	public void generate() {

		String relativePath;
		String fileName;
		String content;

		for (int i = 1; i <= filesToGenerate; i++) {

			relativePath = pathGenerator.generateRandomDirectoryPath();
			fileName = pathGenerator.generateRandomFileName();
			content = pathGenerator.generateString(fileContentLength);

			FileUtils.createFile(path + relativePath + "/" + fileName, content);
		}
	}
}
