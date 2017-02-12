#pragma version(1)
#pragma rs java_package_name(org.mapsforge.map.android)
#pragma rs_fp_relaxed


rs_allocation shadingtex;
rs_sampler sampler;
float mag;
float invmag;
rs_matrix4x4 localmatrix;
void update(float magnitude, rs_matrix4x4 matrix, rs_allocation texture){
    shadingtex = texture;

    mag = magnitude;
    invmag = 1.f-mag;
    localmatrix = matrix;
    //rsMatrixScale(&localmatrix , 1.f/rsAllocationGetDimX(shadingtex), 1.f/rsAllocationGetDimY(shadingtex), 1.f);
    rsDebug("prepared renderscript", &localmatrix);
}

uchar3 __attribute__((kernel)) shadeRGB(uchar3 in, uint32_t x, uint32_t y) {
    float2 coords = {(float)x, (float)y};
    float4 transformed = rsMatrixMultiply(&localmatrix, coords);
    float2 samplecoords = {transformed.x, transformed.y};

    float4 sampled = rsSample(shadingtex, sampler, samplecoords);
if(x%100==0&&y%100==0){
    uint2 coord = {x,y};
   rsDebug("\nsampling at ", coord);
   rsDebug("sampling with ", &localmatrix);
    rsDebug("sampled coords " , samplecoords);
    rsDebug("sampled color " , sampled.a);

}


    uchar3 out;
    if(transformed.x<0.f||transformed.x>1.f||transformed.y<0.f||transformed.y>1.f){
        out = in;
        if(x%5==y%6) out.b=30;
        return out;
    }

    out.r=(uchar)((float)in.r*invmag+255.f*(sampled.a*mag));
    out.g=(uchar)((float)in.g*invmag+255.f*(sampled.a*mag));
    out.b=(uchar)((float)in.b*invmag+255.f*(sampled.a*mag));

    if(y%10>8){
        out = in;
    }

    if(x%100==0&&y%100==0){
        rsDebug(" in color " , in);
        rsDebug("out color " , out);
    }

    return out;
  //uint16_t out = in;
//  out.r = 255 - in.r;
//  out.g = 255 - in.g;
//  out.b = 255 - in.b;
  //return out;
}


ushort __attribute__((kernel)) shade565(ushort in565, uint32_t x, uint32_t y) {
    uchar3 inrgb;
    inrgb.r = (in565 >> 8) & 0xF8;
    inrgb.g = (in565 >> 3) & 0xFC;
    inrgb.b = (in565 << 3) & 0xF8;

    uchar3 outrgb = shadeRGB(inrgb, x, y);
//    uchar3 outrgb = inrgb;
    ushort out565 =
      ((((uint16_t)outrgb.r) & 0xF8) << 8)
    | ((((uint16_t)outrgb.g) & 0xFC) << 3)
    | ((((uint16_t)outrgb.b) & 0xF8) >> 3)
    ;


   
       if(
       false&&
       x%100==0&&y%100==0){
            uint2 coord = {x,y};
           rsDebug("\nlooking at ", coord);
           rsDebug("raw in is ", in565);
           rsDebug("rgb in is ", inrgb);
            rsDebug("rgb out is ", outrgb);
            rsDebug("565 out is ", out565);
       }

    return out565;
}

