/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package customLiFx;

import com.sacide.smart.home.api.compilation.LifxAPI;
import com.sacide.smart.home.api.compilation.backend.Device;
import com.sacide.smart.home.api.compilation.backend.HSVK;
import com.sacide.smart.home.api.compilation.backend.RGBLightDevice;

/**
 *
 * @author saud
 */
public class Main {
	
	public static void main(String[] args) throws Exception {
		/*for(int i=0; i<6; i++) {
			PacketBuilder pb = new PacketBuilder(PacketBuilder.LITTLE);
			pb.addField("long", 64,(i!=0)?0:72623859790382856l, 0);
			pb.addField("bit1", 1, (i!=1)?0:1, 1);
			pb.addField("bit2", 1, (i!=2)?0:1, 1);
			pb.addField("bits", 2, (i!=3)?0:3, 1);
			pb.addField("fill", 4, (i!=4)?0:1, 1);
			pb.addField("int", 32, (i!=5)?0:16909060, 2);
			//System.out.println(i+" "+Arrays.toString(pb.compile()));
			System.out.println(pb);
			pb.parse(pb.compile());
			System.out.println(pb);
			System.out.println();
		}
		System.out.println(BitSetBuilder.long2str(72623859790382856l));
		System.out.println("raw [36, 0, 0, 52, 0, 16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 5, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0]");
		//*/
		
		//System.out.println(HSVK.parseKVSH16(1407656354235023360l));
		
		LifxAPI l = new LifxAPI();
		for(Device d : l.discoverDevices()) {
			RGBLightDevice ld = l.toRGBLightDevice(d);
			System.out.println(ld);
			//ld.setLightPowerState(true, 0);
			ld.setLightColor(HSVK.INCANDESCENT, 0);
			//System.out.println(ld.getLightColor());
			
			// true order kv??
		}
		//*/
		
		// 2500 -> HSVK{0.03814755474174106, 0.15799191271839474, 0.0, 5.9169230769230765}
		// 9000 -> HSVK{0.1373311970702678, 0.029999237048905166, 0.0, 5.9169230769230765}
		
	}
	
}
