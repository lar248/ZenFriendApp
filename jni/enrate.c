/* enrate is a function that uses a
  BW Hz (typ. 16) energy envelope for the wideband speech to estimate
  speaking rate as the time-varying center of mass for the low
  frequency spectrum (skipping the first couple of spectral points,
  which correspond to the offset). The spectral estimation is all
  done on a downsampled envelope, with sampling rate of LOWRATE (typ. 100 Hz).

  This version is explicitly offline; that is, it computes the enrate
  for the complete chunk of data that it is fed.
*/

#include <stdio.h>
#include <math.h>
#include <stdlib.h>
#include "enrate.h"
#include "functions.h"

float enrate (float *fdata, int winlength, int samprate)
{

/*********Declarations and Initializations ***********************/

	/* data arrays */

	static float *lpfdata;
  	static float *anal;
	static float *spec;
	static float *hwindow;
	static float *costable;
	static float *sintable;
	static float *dwndata;

	unsigned int dwnbufsize;
	unsigned int bufsize;

	float cenfreq; /* 1st moment of wideband modulation spectrum */
	float wintime; /* length of analysis window in seconds */
	float ftmp;

	int downby;   /* downsampling factor after envelope detection */
	int nspecvals; /* number of spectral values in BW Hz */
	int dwnwinlength; /* window length in downsampled points */
	double pts_per_hz; /* spectral points per Hz */

	static int first = TRUE;
	static int last_dwnwinlength = -1; /* window length for previous call*/


	int i, j;
	int startfreq = 2;

	
	/* getopt variables */
	int c;
	extern int optind;
  	extern char *optarg;

/************* Range check for input parameters *********/

	if(samprate < LOWRATE)
	{
		fprintf(stderr,"samprate less than %d \n", LOWRATE);
		exit(-1);
	}

	if(samprate > MAXRATE)
	{
		fprintf(stderr,"samprate more than %d \n", MAXRATE);
		exit(-1);
	}

	if(winlength < (.25 * samprate))
	{
		fprintf(stderr,"Warning: enrate running on %d samples \n",
			winlength);
	}

/********** Minor calculations *************/

	/* window length in seconds */
	wintime = (double)winlength / (double)samprate;

	nspecvals = wintime * BW; /* number of spectral values in BW Hz */

	/* window length in sample points for downsampled rate */
	dwnwinlength = wintime * LOWRATE;
	
	/* downsampling ratio for envelope */
	downby = samprate / LOWRATE;

	pts_per_hz = wintime; /* Number of spectral values per Hz,
				equal to the number of seconds in window.  */

/******* Array allocations and initializations ******************/

	dwnbufsize = dwnwinlength+1;
	bufsize = winlength+1;
	/* We allow one for location for filtering space */

	if(dwnwinlength != last_dwnwinlength)
		/* If this is the first time called, or if
			the length is different than before, initialize
			tables */
	{
		if(first != TRUE) free(hwindow);
		hwindow = (float *)calloc(dwnbufsize, sizeof(float));
		if(hwindow == (float *)NULL)
		{
			fprintf(stderr,"Can't allocate mem for hwindow\n");
			exit(-1);
		}
		gethwindow(hwindow, dwnwinlength); 
			/* Hamming window for dwnwinlength */

		if(first != TRUE) free(costable);
		costable = (float *)calloc(dwnbufsize, sizeof(float));
		if(costable == (float *)NULL)
		{
			fprintf(stderr,"Can't allocate mem for costable\n");
			exit(-1);
		}
		getcos (costable, dwnwinlength);/* Trig tables for DFT values */

		if(first != TRUE) free(sintable);
		sintable = (float *)calloc(dwnbufsize, sizeof(float));
		if(sintable == (float *)NULL)
		{
			fprintf(stderr,"Can't allocate mem for sintable\n");
			exit(-1);
		}
		getsin (sintable, dwnwinlength);

		if(first != TRUE) free(dwndata);
		dwndata = (float *)calloc(dwnbufsize, sizeof(float));
		if(dwndata == (float *)NULL)
		{
			fprintf(stderr,"Can't allocate mem for dwndata\n");
			exit(-1);
		}

		if(first != TRUE) free(anal);
		anal = (float *)calloc(dwnbufsize, sizeof(float));
		if(anal == (float *)NULL)
		{
			fprintf(stderr,"Can't allocate mem for anal\n");
			exit(-1);
		}

		if(first != TRUE) free(spec);
		spec = (float *)calloc(dwnbufsize, sizeof(float));
		if(spec == (float *)NULL)
		{
			fprintf(stderr,"Can't allocate mem for spec\n");
			exit(-1);
		}

		if(first != TRUE) free(lpfdata);
		lpfdata = (float *)calloc(bufsize, sizeof(float));
		if(lpfdata == (float *)NULL)
		{
			fprintf(stderr,"Can't allocate mem for lpfdata\n");
			exit(-1);
		}
		first = FALSE;
	}

/************* Now, the signal processing ******************/

	/* For the full data length, half-wave rectify, 
		and low pass the signal at BW Hz (see enrate.h) */
	envelope(&fdata[0], lpfdata, winlength, samprate, BW);
    //printf("files %f %f\n",fdata[0],lpfdata[0]);


	downsample( lpfdata+1, dwndata, winlength, downby);

	vecmult( hwindow, dwndata, anal, dwnwinlength);

	/* get power spectrum up to BW Hz */
	getspec(anal, dwnwinlength, spec, nspecvals, costable, sintable);

/*
 	 ftmp = ceil(pts_per_hz);

	startfreq = (int) ftmp;
*/
	/* Normalize to unity sum, ignoring 1st startfreq values in spectrum*/
	normvec( spec+startfreq, nspecvals - startfreq ); 

	/* compute center of mass for this part of power spectrum */
	cenfreq = getcenfreq(spec, nspecvals, pts_per_hz, startfreq, BW);

	last_dwnwinlength = dwnwinlength;
	return(cenfreq);
}
