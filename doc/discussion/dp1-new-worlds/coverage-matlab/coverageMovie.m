function coverageMovie ()

set(gcf,'Color', 'white');
grid on;
[c,p] = meshgrid(0:.05:1,0:.05:1);
z=0.5+(c.*(p-0.5));
surf(c,p,z)
view(20,12);
pause(0.5);
f = getframe(gcf);
[im,map] = rgb2ind(f.cdata,256,'dither');
 
i=1;
r=30;
x0=50;
y0=12;
for x=-r:2:r
    s=solve('x^2 + y^2 = r^2',['r=' num2str(r)],['x=' num2str(x)]);
    if (length(s.y) > 1)
        y=max(double(s.y(2)),double(s.y(1)))/3;
    else
        y=double(s.y(1))/3;
    end        
    view(x0+x,y0+y);
	f = getframe(gcf);
    im(:,:,1,i) = rgb2ind(f.cdata,map,'dither');
    i=i+1;
end
for x=r:-2:-r
    s=solve('x^2 + y^2 = r^2',['r=' num2str(r)],['x=' num2str(x)]);
    if (length(s.y) > 1)
        y=min(double(s.y(2)),double(s.y(1)))/3;
    else
        y=double(s.y(1))/3;
    end        
    view(x0+x,y0+y);
	f = getframe(gcf);
    im(:,:,1,i) = rgb2ind(f.cdata,map,'dither');
    i=i+1;
end

close(gcf);
imwrite(im,map,'coverage-surface.gif','DelayTime',0,'LoopCount',inf)
