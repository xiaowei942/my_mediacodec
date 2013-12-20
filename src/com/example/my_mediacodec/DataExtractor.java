package com.example.my_mediacodec;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DataExtractor implements Runnable{
	private static final boolean DEBUG = false;
	private static final int MAXSIZE = 20*1024*1024;
	private File file = null;
	private FileInputStream ins = null;
	public static byte[] bytes = new byte[MAXSIZE];
	int file_length;
	String filepath;
	public static boolean prepared = false;
	public static List <Integer> nalu_list = new ArrayList<Integer>();
	
	DataExtractor(String file_path) {
		filepath=file_path;
	}
	
	public void openFile(String file_path) {
		try {
			ins = new FileInputStream(file_path);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			System.out.println("Cannot open input file stream");
			e.printStackTrace();
		}
	}
	
	private int getData(FileInputStream ins) {
		int count = 0;
		try {
			count = ins.read(bytes, 0, MAXSIZE);
			System.out.println("file stream length: " + count);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("Cannot open read file stream");
			e.printStackTrace();
		}
		return count;
	}
	
	private boolean hasNextFrame() {

		return true;
	}
	
	private int getNaluList(byte[] src) {
		for(int i=0; i<file_length; i++) {
			if(src[i] == 0 && src[i+1] == 0) {
				if(src[i+2] == 1) {
					//nalu_list.add(i);
					//i+=2;
				} else if(src[i+2] == 0 && src[i+3] == 1) {
					nalu_list.add(i);
					i+=3;
				}
			}
		}		
		nalu_list.add(file_length);
		
		if(DEBUG) {
			for(int count = 0; count<nalu_list.size(); count++)
				System.out.println("Count: " + count + "  Value: " + nalu_list.get(count).toString());
		}
		return nalu_list.size();
	}
	
	private boolean hasSpsPps(byte[] src) {
		boolean hasSps = false;
		boolean hasPps = false;
		
		int index = nalu_list.get(0);
		for(int j=0; j<5; j++) {
			int temp = src[index+j];
			if( (temp & 0x0f) == 0x07 ) {
				hasSps = true;
				break;
			}
		}
		
		if(hasSps) {
			index = nalu_list.get(1);
			for(int k=0; k<5; k++) {
				int temp = src[index+k];
				if( (temp & 0x0f) == 0x08) {
					hasPps = true;
					return true;
				}
			}
		}
		
		return false;
	}
	
	public static List <Integer> getNaluList() {
		int count = 0;
		//if(hasNextFrame)
		return nalu_list;
	}

	public static byte[] getBytes() {
		return bytes;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		openFile(filepath);
		file_length = getData(ins);

		if(getNaluList(bytes) <= 0) {
			System.out.println("Stream has no nalu info");
		}
		if(hasSpsPps(bytes)) {
			System.out.println("Has Sps and Pps");
		} else {
			System.out.println("Has no Sps or Pps");
		}
		prepared = true;
	}
}
