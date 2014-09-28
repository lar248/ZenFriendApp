#include <math.h>
#include <stdio.h>
#include "functions.h"
#include <stdlib.h>

#ifndef M_PI
#define M_PI           3.14159265358979323846
#endif

/* This routine computes a Hamming window */

void gethwindow( float *window, int winlength)
{
	double base_angle;
        register float tmp;
        int i, j;

        /* Note that M_PI is PI, defined in math.h */
        base_angle =  2.0 * M_PI / (double)(winlength );

	for(i=0; i<winlength; i++)
	{
		window[i] = (.54 - (.46 * cos((double)i * base_angle)));
	}

}

/* This routine computes a small number of spectral magnitude components */
void getspec( float *vecstart, int veclength, float *spec, int nspecvals,
	float *costable, float *sintable)
{
	double base_angle;
	register float sintmp, costmp;
	int i, j, index;
	
	for(i=0; i<nspecvals; i++)
	{
		costmp = 0;
		sintmp = 0;
		for(j=0; j<veclength; j++)
		{
			index = (i * j)% veclength;
			costmp += (*(vecstart+j) * costable[index]);
			sintmp -= (*(vecstart+j) * sintable[index]);
		}
		spec[i] = (costmp*costmp + sintmp*sintmp);
	}
}

void vecmult( float *vec1, float *vec2, float *vecout, int veclength)
{
	int i;

	for(i=0; i<veclength; i++)
	{
		vecout[i] = vec1[i] * vec2[i];
	}
}

float getcenfreq(float *spec, int length, double pts_per_hz, int start, 
int bandwidth)
{
	float tmpwtsum = 0.;
	float tmpsum = 0.;
	float tmpfreq;
	float eps = .000001; /* to prevent division by zero */
	int i;
	int npts;

	npts = (int)((double)bandwidth * pts_per_hz);

	for(i=start; i<npts; i++)
	{
		tmpsum += spec[i];
		tmpwtsum += (spec[i] * i);
	}
	tmpfreq = (tmpwtsum + eps) / (tmpsum + eps);
	tmpfreq /= pts_per_hz;

	return(tmpfreq);
}
	

/* This routine computes a cosine table */
void getcos( float *costable, int veclength)
{
	double base_angle;
	int i;
	
        base_angle =  2.0 * M_PI / (double)(veclength );

	for(i=0; i<veclength; i++)
	{
		costable[i] = cos(base_angle * (double)i );
	}
}

/* This routine computes a sine table */
void getsin( float *sintable, int veclength)
{
	double base_angle;
	int i;
	
        base_angle =  2.0 * M_PI / (double)(veclength );

	for(i=0; i<veclength; i++)
	{
		sintable[i] = sin(base_angle * (double)i );
	}
}

/* Half-wave rectify and lowpass at cutoff Hz 
	to get energy envelope */
void envelope (float *fdata, float *lpfdata, int nsamps, int samprate,
float cutoff)
{
	int i;
	double angle, tmp;
	float ftmp[160000];
	static float mem;
	static int first_call = 1;

	if(first_call)
	{
		angle = 2. * M_PI * cutoff / samprate;
		tmp = 2.0 - cos( angle );
		mem = tmp - sqrt( (tmp*tmp) - 1);
		lpfdata[0] = fdata[0];
	}

    //fprintf(stderr,"files\n");
	for(i=1; i<nsamps; i++)
	{
		/* Half-wave rectify */
		lpfdata[i] = (fdata[i] > 0 ? fdata[i] : 0 );
        //printf("%d %.8f %.8f\n",i,lpfdata[i],fdata[i]);
	}
	lpfdata[nsamps]=0;

	/* Now do single-pole integration with 50 msec time constant */
	for(i=1; i<=nsamps; i++)
	{
		lpfdata[i] = lpfdata[i] + (mem * lpfdata[i-1]);
	}
	lpfdata[0] = lpfdata[nsamps]; /* Save for next time */

	first_call = 0;

}

void normvec( float *vec, int veclength)
{
	int i;
	float sum = 0.0;
	float eps = .000001;

	for(i=0; i<veclength; i++)
	{
		sum += vec[i];
	}
	
	for(i=0; i<veclength; i++)
	{
		vec[i] = ( sum > eps ? vec[i] / sum : eps);
	}
}


/* downsample assumes 1st time series is sufficiently bandlimited so
	that simple downsampling is OK */
void downsample( float *vec1, float *vec2, int length, int downfactor)
{
	int i;

	int index = 0;

	for(i=0; i<length; i+=downfactor)
	{
		vec2[index++] = vec1[i];
	}
}
