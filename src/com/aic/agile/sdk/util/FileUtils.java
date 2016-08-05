package com.aic.agile.sdk.util;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FileUtils 
{
    public static File[] listFiles(String dir, String extension) 
    {
        final String ext = extension;
    	
        File directory = new File(dir);
        if (!directory.isDirectory()) 
        {
            //System.out.println("No directory provided");
            //return null;
        	directory.mkdir();
        }

        File[] files = directory.listFiles(fileFilter(ext));
        //The listFiles method, with or without a filter does not guarantee any order.
        Arrays.sort(files);
        return files;
    }

    public static FileFilter fileFilter(final String extension)
    {
        //create a FileFilter and override its accept-method
	    return new FileFilter() 
	    {
	        public boolean accept(File file) {
	            //if the file extension is .extension return true, else false
	            if (file.getName().endsWith("."+extension)) 
	            {
	                return true;
	            }
	            return false;
	        }
	    };
    }
    
    public static void moveFile(File file, String destination) throws Exception
    {
    	try
    	{
    		//Check destination directory exist or not
    		File fileDir = new File(destination);
    		if (!fileDir.exists()) 
    		{
    			fileDir.mkdirs();
    		}
    		
    		if(file.renameTo(new File(destination + file.getName())))
     	   	{
     		   	//System.out.println("File is moved successful!");
     	   	}
     	   	else
     	   	{
     	   		throw new Exception("Move file failed.");
     	   	}
     	}
    	catch(Exception e)
    	{
     		throw e;
     	}
    }
    
    public static void moveFile2(File afile, String destination) throws Exception
    {
    	InputStream inStream = null;
    	OutputStream outStream = null;
    	
    	try
    	{
    		//Check destination directory exist or not
    		File fileDir = new File(destination);
    		if (!fileDir.exists()) 
    		{
    			fileDir.mkdirs();
    		}
    		
    		File bfile = new File(destination + afile.getName());
 
    	    inStream = new FileInputStream(afile);
    	    outStream = new FileOutputStream(bfile);
 
    	    byte[] buffer = new byte[1024];
 
    	    int length;
    	    //copy the file content in bytes 
    	    while ((length = inStream.read(buffer)) > 0)
    	    {
    	    	outStream.write(buffer, 0, length);
    	    }
 
    	    inStream.close();
    	    outStream.close();
 
    	    //delete the original file
    	    afile.delete();
 
    	    System.out.println("File is copied successful!");
     	}
    	catch(Exception e)
    	{
     		throw e;
     	}
    }
    
    /**
     * Unzip it
     * @param zipFile input zip file
     * @param output zip file output folder
     */
    public static void unZipIt(String zipFile, String outputFolder) throws Exception
    {
    	byte[] buffer = new byte[1024];
 
    	try
    	{
    		//create output directory is not exists
	    	File folder = new File(outputFolder);
	    	if(!folder.exists())
	    	{
	    		folder.mkdir();
	    	}
 
	    	//get the zip file content
	    	ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
	    	//get the zipped file list entry
	    	ZipEntry ze = zis.getNextEntry();
	 
	    	while(ze!=null)
	    	{
	    		String fileName = ze.getName();
	    		File newFile = new File(outputFolder + File.separator + fileName);
	 
		        //System.out.println("file unzip : "+ newFile.getAbsoluteFile());
		 
		        //create all non exists folders
	            //else you will hit FileNotFoundException for compressed folder
	            new File(newFile.getParent()).mkdirs();
	 
	            FileOutputStream fos = new FileOutputStream(newFile);             
	 
	            int len;
	            while ((len = zis.read(buffer)) > 0) 
	            {
	            	fos.write(buffer, 0, len);
	            }
	 
	            fos.close();   
	            ze = zis.getNextEntry();
	    	}
	    	
	    	zis.closeEntry();
		    zis.close();
		 
		    //System.out.println("Done");
		 
		}
    	catch(IOException ex)
    	{
    		throw ex;
		}
    }
    
    public static final Comparator<File> lastModified = new Comparator<File>() {
		@Override
		public int compare(File o1, File o2) 
		{
			return o1.lastModified() == o2.lastModified() ? 0 : (o1.lastModified() < o2.lastModified() ? 1 : -1 ) ;
		}
	};
	/* Sort the files by last modified
	public void testFileSort() throws Exception {
		File[] files = new File(".").listFiles();
		Arrays.sort(files, lastModified);
		System.out.println(Arrays.toString(files));
	}
	*/
}
