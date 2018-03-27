import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.frame.*;
import javax.swing.JButton;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import ij.plugin.HyperStackConverter;
import ij.ImagePlus;
import ij.plugin.Concatenator;
import ij.WindowManager;
import java.text.NumberFormat;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SpinnerModel;
import javax.swing.JPanel;
import javax.swing.JLabel;

public class image_interval extends PlugInFrame{
    boolean isRunning = true;
    ImagePlus imp;
    
    public image_interval(){
        super("AAIIP - Awesome Average Interval Image Plugin");
        JPanel intervalPanel_ = new JPanel(new GridLayout(3,2));
        
        SpinnerModel sModel_interval = new SpinnerNumberModel(
                                                     new Integer(0),
                                                     new Integer(0),
                                                     null,
                                                     new Integer(1)
        );
        
        SpinnerModel sModel_offset = new SpinnerNumberModel(
                                                     new Integer(0),
                                                     new Integer(0),
                                                     null,
                                                     new Integer(1)
                                                     );
        
        final JSpinner numInterval_ = new JSpinner(sModel_interval);
        final JSpinner numOffset_ = new JSpinner(sModel_offset);
        JButton runButton = new JButton("Run");
        runButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                isRunning = true;
                run(((Integer)numInterval_.getValue()), ((Integer)numOffset_.getValue()));
            }
        });
        JButton stopButton = new JButton("Stop");
        stopButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                isRunning = false;
            }
        });
        this.imp = IJ.getImage();
        intervalPanel_.add(new JLabel(" Interval"));
        intervalPanel_.add(new JLabel(" Offset"));
        intervalPanel_.add(numInterval_);
        intervalPanel_.add(numOffset_);
        intervalPanel_.add(runButton);
        intervalPanel_.add(stopButton);
        add(intervalPanel_);
        pack();
        GUI.center(this);
        show();
    }
    
    public int setup(String arg, ImagePlus imp) {
        this.imp = imp;
        return 1;
    }

    public ImagePlus[] extractImage(ImagePlus imp, String path, String origFileName, int dim){
        try{
            ImageStack img_stack = imp.getStack();
            ImageStack[] stack_res = new ImageStack[imp.getNSlices()];
            ImagePlus[] result = new ImagePlus[imp.getNSlices()];
            for(int i = 0 ; i < imp.getNSlices(); i++){
                stack_res[i] = new ImageStack(imp.getWidth(), imp.getHeight());
            }
            int stack_size = img_stack.getSize();
            ImageProcessor ip;
            
            for (int j = 0; j < stack_size; j++) {
                if(!isRunning)
                    return null;
                ip = img_stack.getProcessor(j+1);
                stack_res[j%dim].addSlice(ip);
            }
            for (int k = 0; k < dim; k++){
                if(!isRunning)
                    return null;
                result[k] = new ImagePlus(origFileName + "_Slice_" + k, stack_res[k]);
            }
            return result;
        } catch(Exception e){
            return null;
        }
    }

    public void run(int avgIntervalSlices, int intervalOffset){
        String outName = "";
        int sliceCount = this.imp.getNSlices();
        int totalSlices = this.imp.getImageStackSize();
        String origFilename = imp.getTitle();
        String path = ij.IJ.getDirectory("image");
        ImagePlus[] res = extractImage(imp, path, origFilename, sliceCount);
        
        if (avgIntervalSlices > 0) {
            if(intervalOffset > 0)
                outName += "Offset_" + intervalOffset + "_";
            outName += "Interval_" + avgIntervalSlices + "_";
            ImagePlus[] result = new ImagePlus[sliceCount];
            ij.plugin.Concatenator stackMerger = new ij.plugin.Concatenator();
            ij.plugin.ZProjector z_pro = new ij.plugin.ZProjector();
            
            for(int i = 0; i < sliceCount; i++){
                if(!isRunning)
                    return;

                ImagePlus[] tmpStack = new ImagePlus[(int)Math.ceil((double)res[i].getImageStackSize()/avgIntervalSlices)];
                z_pro.setImage(res[i]);

                int loopStop = (int) Math.ceil((double)res[i].getImageStackSize()/avgIntervalSlices);
                if( intervalOffset > 0){
                    loopStop = (int) Math.ceil((double) (res[i].getImageStackSize()-avgIntervalSlices)/intervalOffset) + 1;
                    tmpStack = new ImagePlus[(int) Math.ceil((double) (res[i].getImageStackSize()-avgIntervalSlices)/intervalOffset) +1];
                }
                for( int c = 0; c < loopStop; c++ ){
                    if(!isRunning)
                        return;

                    int startCounter;
                    int stopCounter;
                    if( intervalOffset > 0 ){
                        startCounter = 1 + c *  intervalOffset;
                        stopCounter = (res[i].getImageStackSize()-(avgIntervalSlices + c* intervalOffset) <= 0)? res[i].getImageStackSize() : avgIntervalSlices + c * intervalOffset;
                    } else {
                        startCounter = 1 + c * avgIntervalSlices;
                        stopCounter = (res[i].getImageStackSize()-avgIntervalSlices*(c+1) <= 0)? res[i].getImageStackSize() : avgIntervalSlices*(c+1);
                    }
                    z_pro.setStartSlice(startCounter);
                    z_pro.setStopSlice(stopCounter);
                    z_pro.doProjection();
                    tmpStack[c] = z_pro.getProjection();
                }
                if( tmpStack != null )
                    result[i] = stackMerger.concatenate(tmpStack, true);
                else
                    return;
            }
            ImagePlus outFile = stackMerger.concatenate(result, true);
            int current_total = outFile.getImageStackSize();
            int current_Slices = result.length;

            outFile = HyperStackConverter.toHyperStack(outFile, 1, current_Slices, current_total/current_Slices, "xytzc", null);
            ij.IJ.saveAsTiff(outFile, path + outName + origFilename);
        }
    }
}