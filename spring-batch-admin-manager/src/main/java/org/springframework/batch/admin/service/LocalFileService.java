/*
 * Copyright 2009-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.admin.service;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.util.Assert;

/**
 * @author Dave Syer
 * 
 */
public class LocalFileService implements FileService, InitializingBean, ResourceLoaderAware {

	private File outputDir = new File(System.getProperty("java.io.tmpdir", "/tmp"), "batch/files");

	private File triggerDir = new File(System.getProperty("java.io.tmpdir", "/tmp"), "batch/triggers");

	private ResourceLoader resourceLoader = new DefaultResourceLoader();

	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	public void afterPropertiesSet() throws Exception {
		if (!outputDir.exists()) {
			Assert.state(outputDir.mkdirs(), "Cannot create output directory " + outputDir);
		}
		Assert.state(outputDir.exists(), "Output directory does not exist " + outputDir);
		Assert.state(outputDir.isDirectory(), "Output file is not a directory " + outputDir);
	}

	public File createFile(String path, String name) throws IOException {

		Assert.state(!name.contains("/") && !name.contains("\\"),
				"The name to create must be a file without a directory (" + name
						+ ").  Use the path parameter to create a directory.");
		File directory = new File(outputDir, path);
		directory.mkdirs();
		Assert.state(directory.exists() && directory.isDirectory(), "Could not create directory: " + directory);

		File dest = File.createTempFile(name + getSuffix(), "", directory);

		return dest;

	}

	public File createTrigger(File dest) throws IOException {
		File file = new File(triggerDir, dest.getAbsolutePath().substring(outputDir.getAbsolutePath().length()));
		FileUtils.writeStringToFile(file, dest.getAbsolutePath());
		return file;
	}

	public List<FileInfo> getFiles(int startFile, int pageSize) throws IOException {

		ResourcePatternResolver resolver = ResourcePatternUtils.getResourcePatternResolver(resourceLoader);
		Resource[] resources = resolver.getResources("file:///" + outputDir.getAbsolutePath() + "/**");

		List<FileInfo> files = new ArrayList<FileInfo>();
		for (int i = startFile; i < startFile + pageSize && i < resources.length; i++) {
			Resource resource = resources[i];
			File file = resource.getFile();
			if (file.isFile()) {
				FileInfo info = new FileInfo(triggerDir, outputDir, file);
				files.add(info);
			}
		}
		Collections.sort(files);

		return files;

	}

	public int deleteAll() throws IOException {

		ResourcePatternResolver resolver = ResourcePatternUtils.getResourcePatternResolver(resourceLoader);
		Resource[] resources = resolver.getResources("file:///" + outputDir.getAbsolutePath() + "/**");

		for (Resource resource : resources) {
			File file = resource.getFile();
			if (file.isFile()) {
				FileUtils.deleteQuietly(file);
			}
		}

		return resources.length;

	}

	public File getTriggerDirectory() {
		return triggerDir;
	}

	public File getUploadDirectory() {
		return outputDir;
	}

	private String getSuffix() {
		return "." + (new SimpleDateFormat("yyyyMMdd").format(new Date())) + ".";
	}

}