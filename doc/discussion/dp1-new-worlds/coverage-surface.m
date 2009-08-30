function coverageSurface()

[c,p] = meshgrid(0:.05:1,0:.05:1);
z=0.5+(c.*(p-0.5));
surf(c,p,z);
