function coverageSurface()

[c,p] = meshgrid(0:.05:1,0:.05:1);
z=0.5+(c.*(p-0.5));
colormap gray(16);
surf(c,p,z);
xlabel('c_T(S)');
ylabel('p_T(W)');
zlabel('p_T(W)');
