package jip.monome.layouts;

public class Intervals {
	public final static IIntervals CHROMATIC = new IIntervals(){
		public Integer getValue(int note, int x, int y, int width, int height) {
			return Integer.valueOf(note + height*x + y);
		}		
	};
	
	public final static IIntervals FOURTHS = new IIntervals(){
		public Integer getValue(int note, int x, int y, int width, int height) {
			return Integer.valueOf(note + 5*x + y);
		}		
	};
	
	public static IIntervals get(String mode){
		if ("fourths".equals(mode)) return FOURTHS;
		if ("chromatic".equals(mode)) return CHROMATIC;
		
		return CHROMATIC;
	}

	public interface IIntervals{
		public Integer getValue(int note, int x, int y, int width, int height);
	}
}
