package com.hocchu.web.service;

import org.springframework.stereotype.Service;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Geometric adherence to the copybook model, not OCR or an assessment of stroke order. */
@Service
public class HandwritingAssessment {
    public static final int SIDE=256;
    private static final int TOLERANCE=5;
    private final Font font;
    private final Map<String,boolean[]> masks=new ConcurrentHashMap<>();
    public record Rating(int score,int coverage,int precision,int missingParts,String feedback) {}
    public HandwritingAssessment() {
        try(var stream=HandwritingAssessment.class.getResourceAsStream("/fonts/handwriting.ttf")) {
            font=Font.createFont(Font.TRUETYPE_FONT,Objects.requireNonNull(stream)).deriveFont(2048f);
        }catch(Exception e){throw new IllegalStateException("Missing copybook font",e);}
    }
    private Shape outline(String text){return font.createGlyphVector(new FontRenderContext(null,true,true),text).getOutline();}
    private static Rectangle2D tight(Shape shape) {
        var iterator=shape.getPathIterator(null,.01);double[] point=new double[6];double left=1e9,top=1e9,right=-1e9,bottom=-1e9;
        while(!iterator.isDone()){int kind=iterator.currentSegment(point);if(kind!=PathIterator.SEG_CLOSE){left=Math.min(left,point[0]);right=Math.max(right,point[0]);top=Math.min(top,point[1]);bottom=Math.max(bottom,point[1]);}iterator.next();}
        return new Rectangle2D.Double(left,top,right-left,bottom-top);
    }
    public BufferedImage template(String letter) {
        String l=letter.toLowerCase(Locale.ROOT);if(!LearningTrackingService.LETTERS.contains(letter.toUpperCase(Locale.ROOT)))throw new IllegalArgumentException("Chữ chưa hỗ trợ.");
        String ref=switch(l){case "ă","â"->"a";case "ê"->"e";case "ô","ơ"->"o";case "ư","i"->"u";default->l;};
        double above=switch(l){case "b","h","k","l"->5;case "d","đ"->4;case "t"->3;case "r","s"->2.5;default->2;};
        double below=switch(l){case "g","y"->3;case "p","q"->2;default->0;};
        Shape original=outline(l);double unit=-tight(outline("a")).getY()/2;Area shape=new Area(original);
        shape.intersect(new Area(new Rectangle2D.Double(-8192,-8192,16384,8192)));
        shape.transform(AffineTransform.getScaleInstance(1/unit,above/-tight(outline(ref)).getY()));
        if(below>0){Area lower=new Area(original);lower.intersect(new Area(new Rectangle2D.Double(-8192,0,16384,8192)));
            lower.transform(AffineTransform.getScaleInstance(1/unit,below/tight(original).getMaxY()));shape.add(lower);}
        if(l.equals("r")) {
            var points=shape.getPathIterator(null,.002);double[] p=new double[6];double x=0,y=0,best=Double.MAX_VALUE;
            while(!points.isDone()){if(points.currentSegment(p)!=PathIterator.SEG_CLOSE){double d=Math.pow(p[0]+.15,2)+Math.pow(p[1]+1,2);if(d<best){best=d;x=p[0];y=p[1];}}points.next();}
            shape.add(new Area(new BasicStroke(.07f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND).createStrokedShape(new Line2D.Double(-.65,-.04,x,y))));
        }
        Rectangle2D bounds=tight(shape);double cell=SIDE/10d;
        AffineTransform transform=new AffineTransform();transform.translate((5.2-bounds.getCenterX())*cell,6*cell);transform.scale(cell,cell);
        BufferedImage image=new BufferedImage(SIDE,SIDE,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();
        g.setColor(Color.WHITE);g.fillRect(0,0,SIDE,SIDE);g.setColor(Color.BLACK);g.fill(transform.createTransformedShape(shape));g.dispose();return image;
    }
    private static boolean[] dark(BufferedImage image) {
        var scaled=new BufferedImage(SIDE,SIDE,BufferedImage.TYPE_INT_RGB);var g=scaled.createGraphics();g.setColor(Color.WHITE);g.fillRect(0,0,SIDE,SIDE);g.drawImage(image,0,0,SIDE,SIDE,null);g.dispose();
        boolean[] result=new boolean[SIDE*SIDE];for(int y=0;y<SIDE;y++)for(int x=0;x<SIDE;x++){int c=scaled.getRGB(x,y);result[y*SIDE+x]=(((c>>16)&255)*299+((c>>8)&255)*587+(c&255)*114)/1000<170;}return result;
    }
    private static int[] distance(boolean[] pixels) {
        int[] d=new int[pixels.length];for(int i=0;i<d.length;i++)d[i]=pixels[i]?0:10000;
        for(int y=0;y<SIDE;y++)for(int x=0;x<SIDE;x++){int i=y*SIDE+x;if(x>0)d[i]=Math.min(d[i],d[i-1]+1);if(y>0)d[i]=Math.min(d[i],d[i-SIDE]+1);if(x>0&&y>0)d[i]=Math.min(d[i],d[i-SIDE-1]+1);if(x+1<SIDE&&y>0)d[i]=Math.min(d[i],d[i-SIDE+1]+1);}
        for(int y=SIDE-1;y>=0;y--)for(int x=SIDE-1;x>=0;x--){int i=y*SIDE+x;if(x+1<SIDE)d[i]=Math.min(d[i],d[i+1]+1);if(y+1<SIDE)d[i]=Math.min(d[i],d[i+SIDE]+1);if(x+1<SIDE&&y+1<SIDE)d[i]=Math.min(d[i],d[i+SIDE+1]+1);if(x>0&&y+1<SIDE)d[i]=Math.min(d[i],d[i+SIDE-1]+1);}
        return d;
    }
    public Rating assess(String letter,BufferedImage drawing) {
        boolean[] target=masks.computeIfAbsent(letter.toUpperCase(Locale.ROOT),key->dark(template(key))),ink=dark(drawing);
        int[] toTarget=distance(target),toInk=distance(ink);int targetSize=0,inkSize=0,covered=0,inside=0;
        for(int i=0;i<ink.length;i++){if(target[i]){targetSize++;if(toInk[i]<=TOLERANCE)covered++;}if(ink[i]){inkSize++;if(toTarget[i]<=TOLERANCE)inside++;}}
        double coverage=targetSize==0?0:covered/(double)targetSize,precision=inkSize<4?0:inside/(double)inkSize;
        int score=coverage+precision==0?0:(int)Math.round(200*coverage*precision/(coverage+precision));
        boolean[] seen=new boolean[target.length];int missing=0;
        for(int start=0;start<target.length;start++)if(target[start]&&!seen[start]) {
            var queue=new ArrayDeque<Integer>();queue.add(start);seen[start]=true;int size=0,hit=0;
            while(!queue.isEmpty()){int p=queue.removeFirst();size++;if(toInk[p]<=TOLERANCE)hit++;
                for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++){int x=p%SIDE+dx,y=p/SIDE+dy;if(x>=0&&x<SIDE&&y>=0&&y<SIDE){int n=y*SIDE+x;if(target[n]&&!seen[n]){seen[n]=true;queue.add(n);}}}}
            if(size>=5&&hit/(double)size<.45)missing++;
        }
        if(missing>0)score=(int)Math.round(score*.5);
        String message=missing>0?"Bé bổ sung dấu hoặc phần nét còn thiếu nhé.":score>=80?"Nét chữ bám mẫu tốt. Bé tiếp tục phát huy nhé!":precision<.65?"Bé đưa nét viết sát mẫu và giữ đúng vị trí trên ô li nhé.":"Bé viết đủ thân chữ, nét nối và dấu còn thiếu nhé.";
        return new Rating(score,(int)Math.round(coverage*100),(int)Math.round(precision*100),missing,message);
    }
}
