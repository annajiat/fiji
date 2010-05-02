/*
 * LSM_Toolbox.java
 *
 * @author Patrick Pirrotte
 *
 * Created on february 2002 Copyright (C) 2002-2009 Patrick Pirrotte
 *
 * ImageJ plugin
 * Version	:      4.0g
 * Authors  :      Patrick PIRROTTE, Jerome MUTTERER
 * Licence  :	   GPL 
 * 
 * This software includes images from the Nuvola iconset which was released under LGPL v2.1, please see
 * iconset license
 *
 * Emails   :      patrick@image-archive.org (project maintainer)
 *                 jerome.mutterer@ibmp-ulp.u-strasbg.fr
 *
 * Description :   This plugin reads *.lsm files produced by Zeiss LSM 510 confocal microscopes.
 *                 Each channel of an image plane, stack or time series is opened as a separate
 *                 image or stack window. The plugin also retrieves calibration infos from
 *                 LSM files. This plugin has been built using Zeiss' v4.0 fileformat
 *                 specifications. This software is compatible with files generated with AIM version 4.0.
 *                 Other versions of the lsm format should be readable more or less well.
 *                 A short manual is available from
 *                 ibmp.u-strasbg.fr/sg/microscopie/methods/lsmio/lsmio.htm
 *
 * To run from a plugin:
 * runPlugIn("LSM_Toolbox","file=path_to_file");
 * To run from a macro:
 * call("LSM_Toolbox.open","file=path_to_file")
 * 
 * __________________________________________________________________________
 * (C) 2003-2008 Patrick Pirrotte, Jérôme Mutterer
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

import ij.IJ;
import ij.ImagePlus;
import ij.Macro;
import ij.macro.ExtensionDescriptor;
import ij.macro.Functions;
import ij.macro.MacroExtension;
import ij.plugin.PlugIn;

import javax.swing.JFrame;

import org.imagearchive.lsm.toolbox.BatchConverter;
import org.imagearchive.lsm.toolbox.DomXmlExporter;
import org.imagearchive.lsm.toolbox.MasterModel;
import org.imagearchive.lsm.toolbox.Reader;
import org.imagearchive.lsm.toolbox.ServiceMediator;
import org.imagearchive.lsm.toolbox.StampUtils;
import org.imagearchive.lsm.toolbox.gui.AboutDialog;
import org.imagearchive.lsm.toolbox.gui.ControlPanelFrame;

public class LSM_Toolbox implements PlugIn, MacroExtension {

	public MasterModel masterModel = MasterModel.getMasterModel();

	public final String infoMessage = "LSM_Toolbox" + MasterModel.VERSION
			+ " Copyright (C) 2002-2010 P. Pirrotte, J. Mutterer\n\n"
			+ "This software is subject to the GNU General Public License\n"
			+ "Please read LICENSE or read source code information headers\n"
			+ "Works on images generated by LSM 510 version 2.8 to 4.0\n"
			+ "Contacts :\n" + "patrick@image-archive.org and "
			+ "jerome.mutterer@ibmp-ulp.u-strasbg.fr\n";

	private ExtensionDescriptor[] extensions = { ExtensionDescriptor
			.newDescriptor("lsmXML", this, ARG_STRING+ARG_OUTPUT),
			ExtensionDescriptor
			.newDescriptor("getTStamps", this, ARG_STRING+ARG_OUTPUT),
			ExtensionDescriptor
			.newDescriptor("getZStamps", this, ARG_STRING+ARG_OUTPUT),
			ExtensionDescriptor
			.newDescriptor("getLStamps", this, ARG_STRING+ARG_OUTPUT),
			ExtensionDescriptor
			.newDescriptor("getEvents", this, ARG_STRING+ARG_OUTPUT),
			ExtensionDescriptor
			.newDescriptor("lsmOpen", this, ARG_STRING)		
	};
	
	public ControlPanelFrame controlPanel;

	public void run(String args) {
		IJ.register(LSM_Toolbox.class);
		MasterModel.debugMode = IJ.debugMode;
		if (args.equals("about")) {
			new AboutDialog(new JFrame(), masterModel).setVisible(true);
			return;
		}
		if (IJ.versionLessThan("1.41a"))
			return;
		
		String fileName = "";
		String macroOptions = Macro.getOptions();
		if (IJ.macroRunning())
		if (macroOptions.trim().equalsIgnoreCase("ext")){
				    Functions.registerExtensions(this);
				return;
		} 
		if (!args.equals(""))
			fileName = getMacroOption("file=", args);
		if (macroOptions != null && (!macroOptions.equals("")))
			fileName = getMacroOption("file=", macroOptions).trim();

		if (!fileName.equals("") && fileName.endsWith(".lsm")) {

			final String fn = fileName;
			final Reader reader = ServiceMediator.getReader();
			try {
				IJ.showStatus("Loading image");
				ImagePlus imp = reader.open(fn, true);
				IJ.showStatus("Image loaded");
				if (imp == null)
					return;
				imp.setPosition(1, 1, 1);
				imp.show();
			} catch (OutOfMemoryError e) {
				IJ.outOfMemory("Could not load lsm image.");
			}

		} else if (fileName.endsWith(".csv")) {
			BatchConverter converter = new BatchConverter(masterModel);
			converter.convertBatchFile(args);

		} else if (args.equals("")) {
			controlPanel = new ControlPanelFrame(masterModel);
			controlPanel.initializeGUI();
		}
	}

	public String getMacroOption(String tag, String options) {
		int index = options.indexOf(tag);
		if (index == -1)
			return null;
		return options.substring(index + 5, options.length());
	}

	// Use this method to open from a macro
	public static void open(String args) {
		new LSM_Toolbox().run(args);
	}

	public static String getXML(String filename, boolean filter) {
		return new DomXmlExporter().getXML(filename, filter);
	}
	
	/*public static CZLSMInfoExtended getCZ(String filename) {
		Reader reader = ServiceMediator.getReader();
		ImagePlus imp = reader.open(filename, false);
		reader.updateMetadata(imp);
		LSMFileInfo lsm = (LSMFileInfo) imp.getOriginalFileInfo();
		ImageDirectory imDir = (ImageDirectory) lsm.imageDirectories.get(0);
		CZLSMInfoExtended cz = (CZLSMInfoExtended) imDir.TIF_CZ_LSMINFO;
		return cz;
	}*/
	
	public ExtensionDescriptor[] getExtensionFunctions() {
		return extensions;
	}

	public String handleExtension(String name, Object[] args) {
		Object o = args[0];
		if (name.equals("lsmXML")) {
			if (o == null)
				return null;
			
			String[] a =(String[])o;
			return new DomXmlExporter().getXML(a[0], false);
		}
		if (name.equals("lsmOpen")) {
			if (o == null)
				return null;
			ImagePlus imp = ServiceMediator.getReader().open((String)o, false);
			if (imp == null)
				return null;
			imp.setPosition(1, 1, 1);
			imp.show();
			return null;
		}
		if (name.equals("getTStamps")) {
			if (o == null)
				return null;
			String[] a =(String[])o;
			Reader reader = ServiceMediator.getReader();
			ImagePlus imp = reader.open(a[0], false);
			if (imp==null) return null;
			return StampUtils.getTStamps(reader, imp);
		}
		if (name.equals("getLStamps")) {
			if (o == null)
				return null;
			String[] a =(String[])o;
			Reader reader = ServiceMediator.getReader();
			ImagePlus imp = reader.open(a[0], false);
			return StampUtils.getLStamps(reader, imp);
		}
		if (name.equals("getZStamps")) {
			if (o == null)
				return null;
			String[] a =(String[])o;
			Reader reader = ServiceMediator.getReader();
			ImagePlus imp = reader.open(a[0], false);
			if (imp==null) return null;
			return StampUtils.getZStamps(reader, imp);
		}
		if (name.equals("getEvents")) {
			if (o == null)
				return null;
			String[] a =(String[])o;
			Reader reader = ServiceMediator.getReader();
			ImagePlus imp = reader.open(a[0], false);
			if (imp==null) return null;
			return StampUtils.getEvents(reader, imp);
		}
		return null;
	}
}
