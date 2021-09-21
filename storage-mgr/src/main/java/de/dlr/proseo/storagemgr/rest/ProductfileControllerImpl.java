package de.dlr.proseo.storagemgr.rest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.storagemgr.StorageManagerConfiguration;
import de.dlr.proseo.storagemgr.rest.model.RestFileInfo;
import de.dlr.proseo.storagemgr.utils.StorageType;
import de.dlr.proseo.storagemgr.utils.ProseoFile;
import de.dlr.proseo.storagemgr.utils.StorageLogger;

/**
 * Spring MVC controller for the prosEO Storage Manager; implements the services
 * required to manage product files
 * 
 * @author Ernst Melchinger
 *
 */

@Component
public class ProductfileControllerImpl implements ProductfileController {

	private static final String HTTP_HEADER_WARNING = "Warning";
	private static final String HTTP_MSG_PREFIX = "199 proseo-storage-mgr ";

	private static final String MSG_EXCEPTION_THROWN = "(E%d) Exception thrown: %s";
	private static final String MSG_FILE_NOT_FOUND = "(E%d) File %s not found";
	private static final String MSG_FILE_COPIED = "(I%d) Requested object %s copied to target path %s";
	private static final String MSG_TARGET_PATH_MISSING = "(E%d) No target path given";
	private static final String MSG_FILES_UPDATED = "(I%d) Product file %s uploaded for product ID %d";

	private static final int MSG_ID_EXCEPTION_THROWN = 4051;
	private static final int MSG_ID_FILE_COPIED = 4052;
	private static final int MSG_ID_FILE_NOT_FOUND = 4053;
	private static final int MSG_ID_FILES_UPDATED = 4054;

	// Same as in ProseFileS3
	private static final int MSG_ID_TARGET_PATH_MISSING = 4100;

	private static final String MSG_FILE_NOT_FETCHED = "Requested file {} not copied";

	private static Logger logger = LoggerFactory.getLogger(ProductfileControllerImpl.class);

	@Autowired
	private StorageManagerConfiguration cfg;

	/**
	 * Create an HTTP "Warning" header with the given text message
	 * 
	 * @param message
	 *            the message text
	 * @return an HttpHeaders object with a warning message
	 */
	private HttpHeaders errorHeaders(String message) {
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_WARNING, HTTP_MSG_PREFIX + (null == message ? "null" : message.replaceAll("\n", " ")));
		return responseHeaders;
	}

	/**
	 * Copy source file named pathInfo to file cache used by processors. The
	 * local file name is: posixWorkerMountPoint + relative source file path
	 * 
	 * @param pathInfo
	 *            Source file name
	 * @return Local file name
	 */
	@Override
	public ResponseEntity<RestFileInfo> getRestFileInfoByPathInfo(String pathInfo) {

		if (logger.isTraceEnabled())
			logger.trace(">>> getRestFileInfoByPathInfo({})", pathInfo);

		if (null == pathInfo || pathInfo.isBlank()) {
			return new ResponseEntity<>(
					errorHeaders(StorageLogger.logError(logger, MSG_TARGET_PATH_MISSING, MSG_ID_TARGET_PATH_MISSING)),
					HttpStatus.BAD_REQUEST);
		}

		ProseoFile sourceFile = ProseoFile.fromPathInfo(pathInfo, cfg);
		ProseoFile targetFile = ProseoFile.fromPathInfo(cfg.getPosixWorkerMountPoint() + "/" + sourceFile.getRelPathAndFile(), cfg);

		try {
			ArrayList<String> transferredFiles = sourceFile.copyTo(targetFile, false);
			if (transferredFiles != null && !transferredFiles.isEmpty()) {
				RestFileInfo response = new RestFileInfo();
				response.setStorageType(targetFile.getFsType().toString());
				response.setFilePath(targetFile.getFullPath());
				response.setFileName(targetFile.getFileName());
				response.setFileSize(targetFile.getLength());

				StorageLogger.logInfo(logger, MSG_FILE_COPIED, MSG_ID_FILE_COPIED, sourceFile.getFullPath(), targetFile.getFullPath());

				return new ResponseEntity<>(response, HttpStatus.OK);
			} else {
				return new ResponseEntity<>(
						errorHeaders(StorageLogger.logError(logger, MSG_FILE_NOT_FOUND, MSG_ID_FILE_NOT_FOUND, pathInfo)),
						HttpStatus.NOT_FOUND);
			}
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<RestFileInfo>(errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (Exception e) {
			return new ResponseEntity<>(errorHeaders(StorageLogger.logError(logger, MSG_EXCEPTION_THROWN, MSG_ID_EXCEPTION_THROWN,
					e.getClass().toString() + ": " + e.getMessage())), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Copy local file named pathInfo to storage manager. The target file path
	 * is: default mount point + productId + relative source file path
	 * 
	 * @param pathInfo
	 *            Source file name
	 * @param productId
	 *            Product id
	 * @return Target file name
	 */
	@Override
	public ResponseEntity<RestFileInfo> updateProductfiles(String pathInfo, Long productId, Long fileSize) {

		if (logger.isTraceEnabled())
			logger.trace(">>> updateProductfiles({}, {})", pathInfo, productId);

		RestFileInfo response = new RestFileInfo();
		if (pathInfo != null) {
			ProseoFile sourceFile = ProseoFile.fromPathInfo(pathInfo, cfg);
			ProseoFile targetFile = ProseoFile.fromType(StorageType.valueOf(cfg.getDefaultStorageType()),
					String.valueOf(productId) + "/" + sourceFile.getFileName(), cfg);
			try {
				// wait until source file is really copied
				if (sourceFile.getFsType() == StorageType.POSIX) {
					int i = 0;
					Path fp = Path.of(sourceFile.getFullPath());
					if (fp.toFile().isFile()) {
						Integer wait = Integer.valueOf(cfg.getFileCheckWaitTime());
						Integer max = Integer.valueOf(cfg.getFileCheckMaxCycles());
						try {
							synchronized (this) {
								while (Files.size(fp) < fileSize && i < max) {
									if (logger.isDebugEnabled()) {
										logger.debug("Wait for fully copied file {}", sourceFile.getFullPath());
									}
									i++;
									try {
										this.wait(wait);
									} catch (InterruptedException e) {
										// Do nothing (except for debug
										// logging), we just stay in the while
										// loop
										if (logger.isDebugEnabled())
											logger.debug("... wait interrupted, cause: " + e.getMessage());
									}
								}
							}
						} catch (IOException e) {
							logger.error("Unable to access file {}", sourceFile.getFullPath());
							return new ResponseEntity<>(errorHeaders(StorageLogger.logError(logger, MSG_EXCEPTION_THROWN,
									MSG_ID_EXCEPTION_THROWN, e.getClass().toString() + ": " + e.getMessage())),
									HttpStatus.INTERNAL_SERVER_ERROR);
						}
						if (i >= max) {
							logger.error(MSG_FILE_NOT_FETCHED, sourceFile.getFullPath());
						}
					}
				}
				ArrayList<String> transfered = sourceFile.copyTo(targetFile, false);

				if (transfered != null && !transfered.isEmpty()) {
					response.setStorageType(targetFile.getFsType().toString());
					response.setFilePath(targetFile.getFullPath());
					response.setFileName(targetFile.getFileName());
					response.setFileSize(targetFile.getLength());

					StorageLogger.logInfo(logger, MSG_FILES_UPDATED, MSG_ID_FILES_UPDATED, pathInfo, productId);
					
					return new ResponseEntity<>(response, HttpStatus.CREATED);
				}
			} catch (Exception e) {
				return new ResponseEntity<>(errorHeaders(StorageLogger.logError(logger, MSG_EXCEPTION_THROWN,
						MSG_ID_EXCEPTION_THROWN, e.getClass().toString() + ": " + e.getMessage())),
						HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}
		return new ResponseEntity<RestFileInfo>(response, HttpStatus.NOT_FOUND);
	}
}
