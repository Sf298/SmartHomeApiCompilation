/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sacide.smart.home.api.compilation.backend;

import java.awt.Color;
import java.text.DecimalFormat;

/**
 *
 * @author saud
 */
public class HSVK {
	
	public static final HSVK DAYLIGHT =       new HSVK(0.0000, 0.0000, null, 0.3846);
    public static final HSVK COOL_WHITE =     new HSVK(0.0000, 0.0000, null, 0.2308);
    public static final HSVK ALICE_BLUE =     new HSVK(0.5778, 0.0588, null, 0.0308);
    public static final HSVK INCANDESCENT =   new HSVK(0.5742, 0.0000, null, 0.0308);
    public static final HSVK CRIMSON =        new HSVK(0.9667, 0.9091, null, 0.7692);
    public static final HSVK MAGENTA =        new HSVK(0.8333, 1.0000, null, 0.7692);
    public static final HSVK INDIGO =         new HSVK(0.7628, 1.0000, null, 0.7692);
    public static final HSVK LIGHT_SKY_BLUE = new HSVK(0.5638, 0.4600, null, 0.7692);
    public static final HSVK AZURE =          new HSVK(0.5837, 1.0000, null, 0.7692);
    public static final HSVK AQUAMARINE =     new HSVK(0.4440, 0.5020, null, 0.7692);
    public static final HSVK LIGHT_GREEN =    new HSVK(0.3333, 0.3949, null, 0.7692);
    public static final HSVK FOREST_GREEN =   new HSVK(0.3333, 0.7554, null, 0.7692);
	
    private static HSVK[] colorList = new HSVK[] {DAYLIGHT, COOL_WHITE,
        ALICE_BLUE, INCANDESCENT, CRIMSON, MAGENTA, INDIGO, LIGHT_SKY_BLUE,
        AZURE, AQUAMARINE, LIGHT_GREEN, FOREST_GREEN};
    
	public static HSVK random() {
        return colorList[(int)(Math.random()*colorList.length)];
    }
	
	
	private Double hue;
	private Double sat;
	private Double val;
	private Double kel;
	
	public HSVK() {}
	public HSVK(Double hue, Double sat, Double val, Double kel) {
		this.hue = hue;
		this.sat = sat;
		this.val = val;
		this.kel = kel;
	}

	public int getHue(int scaleMax) {
		return (int) (hue * scaleMax);
	}
	public int getHue(int minVal, int maxVal) {
		return (int) (hue * (maxVal-minVal) + minVal);
	}
	public double getHue() {
		return hue;
	}
	public void setHue(int hue, int scaleMax) {
		this.hue = hue/(double)scaleMax;
	}

	public int getSat(int scaleMax) {
		return (int) (sat * scaleMax);
	}
	public int getSat(int minVal, int maxVal) {
		return (int) (sat * (maxVal-minVal) + minVal);
	}
	public double getSat() {
		return sat;
	}
	public void setSat(int sat, int scaleMax) {
		this.sat = sat/(double)scaleMax;
	}

	public int getVal(int scaleMax) {
		return (int) (val * scaleMax);
	}
	public int getVal(int minVal, int maxVal) {
		return (int) (val * (maxVal-minVal) + minVal);
	}
	public double getVal() {
		return val;
	}
	public void setVal(int val, int scaleMax) {
		this.val = val/(double)scaleMax;
	}

	public int getKel(int scaleMax) {
		return (int) (kel * scaleMax);
	}
	public int getKel(int minVal, int maxVal) {
		return (int) (kel * (maxVal-minVal) + minVal);
	}
	public double getKel() {
		return kel;
	}
	public void setKel(int kel, int scaleMax) {
		this.kel = kel/(double)scaleMax;
	}
	
	public long asHSVK16() {
		long h = getHue(1, 65535);
		long s = getSat(1, 65535);
		long v = getVal(1, 65535);
		long k = getKel(2500, 9000);
		return (h << 48) | (s << 32) | (v << 16) | k;
	}
	public long asKVSH16() {
		long h = getHue(1, 65535);
		long s = getSat(1, 65535);
		long v = getVal(1, 65535);
		long k = getKel(2500, 9000);
		return (k << 48) | (v << 32) | (s << 16) | h;
	}
	public Color asColor() {
		return Color.getHSBColor((float)(double)hue, (float)(double)sat, (float)(double)val);
    }
	
	public static HSVK parseColor(Color c) {
		return parseColor(c.getRed(), c.getGreen(), c.getBlue());
	}
	public static HSVK parseColor(int r, int g, int b) {
		float[] out = Color.RGBtoHSB(r, g, b, null);
		return new HSVK((double)out[0], (double)out[1], (double)out[2], null);
	}
	public static HSVK parseHSVK16(long hsvk16) {
		HSVK out = new HSVK();
		out.hue = toRange(getQuarter(hsvk16, 3), 1, 65535);
		out.sat = toRange(getQuarter(hsvk16, 2), 1, 65535);
		out.val = toRange(getQuarter(hsvk16, 1), 1, 65535);
		out.kel = toRange(getQuarter(hsvk16, 0), 2500, 9000);
		return out;
	}
	public static HSVK parseKVSH16(long kvsh16) {
		HSVK out = new HSVK();
		out.hue = toRange(getQuarter(kvsh16, 0), 1, 65535);
		out.sat = toRange(getQuarter(kvsh16, 1), 1, 65535);
		out.val = toRange(getQuarter(kvsh16, 2), 1, 65535);//((hsvk16 >>> 48)-1)/65534.0;
		out.kel = toRange(getQuarter(kvsh16, 3), 2500, 9000);//((hsvk16 >>> 48)-1)/65534.0;
		return out;
	}
	private static int getQuarter(long val, int part) {
		int shiftAmount = part*16;
		val = val >>> shiftAmount;
		val = val & 0xFFFF;
		return (int)val;
	}
	private static int toRange(int val, int fromMin, int fromMax, int toMin, int toMax) {
		double d = toRange(val, fromMin, fromMax);
		return toRange(d, toMin, toMax);
	}
	private static double toRange(int val, int fromMin, int fromMax) {
		return (val-fromMin)/(double)(fromMax-fromMin);
	}
	private static int toRange(double val, int toMin, int toMax) {
		return (int) (val * (toMax-toMin) + toMin);
	}
	
	
	public boolean hasNullValue() {
		return hue==null || sat==null || val==null || kel==null;
	}
	public void fillNullWith(HSVK hsvk) {
		if(hue==null) hue = hsvk.hue;
		if(sat==null) sat = hsvk.sat;
		if(val==null) val = hsvk.val;
		if(kel==null) kel = hsvk.kel;
	}

	@Override
	public String toString() {
		DecimalFormat df = new DecimalFormat("#.####");
		return "HSVK{" + df.format(hue) + ", " + df.format(sat) + ", " + df.format(val) + ", " + df.format(kel) + '}';
	}
	
}
