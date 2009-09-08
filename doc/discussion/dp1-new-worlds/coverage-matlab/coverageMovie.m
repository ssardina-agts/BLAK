function coverageMovie ()

fig=figure;
set(fig,'Color', 'white');
grid on;

aviobj = avifile('coverage-surface.avi');
aviobj.quality = 100;
aviobj.fps = 15;

[c,p] = meshgrid(0:.05:1,0:.05:1);
z=0.5+(c.*(p-0.5));
h = surf(c,p,z);

for k=1:.5:25
    hold on
    axis([0 1 0 1 0 1])
    view(45+k,37-k);
    hold off
    F = getframe(fig);
    aviobj = addframe(aviobj,F);
end
%close(fig)
aviobj = close(aviobj);
