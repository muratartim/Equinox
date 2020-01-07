#! /opt/perl5/bin/perl -w 

use strict;
use POSIX qw(log10 ceil floor);
#use Tk;
                        
my $self      = {}; # Program to convert an STH file to a SIGMA File
$self->{date} = '03/02/2012';
print STDERR " * convert SFEST STH 2 SIGMA with SEQUENCE using Sequence File\n";
print STDERR " * A Material Header File is Required during the SIGMA conversion\n";
print STDERR " *  ------>>>>>> Run saferun if requested by    Mr. USER!\n";
print STDERR " *  ------>>>>>> Material Data for 7010 embedded in Tool!\n\n";

############################ USER INPUT #####################################
$self->{sth_file}    = $ARGV[0];
$self->{fts_file}    = $ARGV[1];        # Class Sequence File. Not used if $self->{use_ft_sequence_in_sth_as_is}  >  0;
$self->{run_saferun} = 0; 
$self->{use_ft_sequence_in_sth_as_is}  = 0;      # i.e. > 0 if Flights are already in correct sequence!  Then PROVIDE expected TOTAL Flights in STH ! i.e. A400M 1000; A380 1900, etc
$self->{use_this_material} = 'dummy_material';
$self->{analysis_type} = $ARGV[2];
#########################################################################

open (RESULTS,  ">> ZZZ_RESULTS_CGROWTH_SAFE_SIGMA.log");
&begin_global_conversion_process($self);
close(RESULTS);
print STDERR " * ... end of process\n";


sub begin_global_conversion_process
  {
    my $self     = shift;
    my $name     = $self->{sth_file};  
    $name =~s/\.st[1|h]/\.sigma/; 
    print STDERR "NAME       ----->>>>>>>>   $name";
    unless ($name =~m/sigma/) {$name = $name . '.sigma';}
    $self->{sigma_file}   = $name;
    print STDERR " * loading *sth  file : $self->{sth_file} \n";
    &load_all_sth_data($self);

    print STDERR " * loading sequence   : $self->{fts_file} \n";
    &load_flight_type_sequence_data($self);

    print STDERR " * writing *sigma_file: $self->{sigma_file} ......\n";
    &add_safe_material_7010_header($self);  # 7010 from SAFE    
    &write_sigma_file_in_sequence($self);

    if ($self->{run_saferun} > 0)  {&do_auto_run_SAFERUN($self);}
  }

  

sub do_auto_run_SAFERUN
  {
    my $self     = shift;
    my $sig_file = $self->{sigma_file};    
    my $dossier  = $self->{sth_file};  
    $dossier     =~s/\.st[1|h]/\.dossier/; 
	
    system ("rm $dossier") if -e $dossier; # remove old files
    system ("safe_run aspectre $sig_file");
	
    if (-e $dossier) 
      {
	    my $row = `tail -n 2 $dossier | head -n 1`;  
		chomp($row);
	    $row =~ s/^\s+//;
        $row =~ s/\s+$//;
        my @results = split(':', $row);
        my $seq = $results[$#results];
        $seq =~ s/^\s+//; $seq =~ s/\s+$//;
        if (defined $seq) {printf RESULTS sprintf("%-50s %10.2f %s", "$dossier","$seq","\n");} else  {print RESULTS "\n*** $sig_file: no seq generated\n"; }
      }
	  
    unless (-e $dossier) {print RESULTS "\n*** $sig_file: no dossier file found\n";}
  }

  
sub write_sigma_file_in_sequence
  {
    my $self          = shift;
    my $j             = 0;

    my $mission_validity = $self->{VALID_FOR} / 1; 
    #my $mission_validity = $self->{VALID_FOR} / 10; 	                  # REQUIRED for LEGACY where CDFs validity is 1 DSG!
    print STDERR " ** WARNING -> Check if VALIDITY of MISSION is Correctly INTEPRETED!!!\n";

    open( SIGMAOUT, ">> $self->{sigma_file}" );

    print SIGMAOUT sprintf("%5s%7i ! TOTAL NUMBER OF FLIGHTS %s", "NBVOL","$mission_validity","\n");

    foreach (@{$self->{sequence_array}})
      {
	my $ftype_num     = $_;
	my $name          = $self->{SEQ}->{$ftype_num}->{name_number};
	die unless (defined $name);  # please check sequence file!
	my @values;
	my $valid_flights = $self->{$name}->{valid};
	my $block         = $self->{$name}->{block};
	my $stress_points = $self->{$name}->{steps};
	my $i             = 0;
	$j                = $j + 1;

	print SIGMAOUT sprintf("%5s%7i ! FLIGHT NUMBER %s", "NUVOL","$j","\n");
	print SIGMAOUT sprintf("%15s%7i %s", "TITLE FLIGHT NB","$j","\n");
	print SIGMAOUT sprintf("%5s%7i  %s", "NBVAL","$stress_points","\n");
#	print STDERR   sprintf("%5i %15s %s", "$j","$name","\n");

	for (1 .. $stress_points)
	  {
	    $i  = $_;

	    push(@values, $self->{$name}->{values}->{$i});
	  }

	$i = 0;

	foreach (@values)
	  {
	    my $value = $_;
	    $i++;
	    print SIGMAOUT sprintf("%14.4f", "$value");

	    if ($i == 10)
	      {
		$i  = 0;
		print SIGMAOUT "\n";	
	      }
	  }

	unless ($i == 0)
	  {
	    $i  = 0;
	    print SIGMAOUT "\n";
	  }

	print SIGMAOUT "\n";
      }

    close(SIGMAOUT);
  }


sub load_all_sth_data
  {
    my $self   = shift;
    my $stress_factor = 1.0;
    my $column = 8.0;
    my $sum    = 0.0;
    my $sth    = $self->{sth_file};
    my $z      = 0;

    $self->{maximum_stress} = -1000000;
    $self->{minimum_stress} =  1000000;

    @{$self->{header}} = ();
    @{$self->{names}} = ();
	$self->{SEQ} = {};

    open( INPUT, "< $sth" );
    my @contents    = <INPUT>;
    chop(@contents);
    close(INPUT);

    foreach (0 .. 3) 
      {
	my $line = shift(@contents);
	push(@{$self->{header}}, $line);
      }

    for(1 .. 10001)                     # Max FT number expected.
      {
	my $i = $_;
	
	my $line = shift(@contents);
	last if (!defined $line);

	$line =~ s/^\s*//g;

	my ($valid_flight, $blocks) = split(/\s+/, $line);

	$line = shift(@contents);
	$line =~ s/^\s*//g;
	
	my ($steps, $name)          = split(/\s+/, $line);

	$steps              =~s/\s*//;
    $name               = 'TF_' . $i  unless (defined $name);
	$name               =~s/\s*//g;

	push(@{$self->{names}}, $name);
	$self->{$name}->{steps} = $steps;
	$self->{$name}->{valid} = $valid_flight;
	$self->{$name}->{block} = $blocks;
	
	$self->{SEQ}->{$i}->{name_number} = $name;

	my $rows  = $steps / $column;
	my $int   = floor($rows); # gives the Interger part of Number
	my $dig   = ceil($rows);  # makes 30.25 to become 31
	$rows     = $dig;
	
	for (1 .. $rows) 
	  {
	    $line = shift(@contents);
	    push(@{$self->{$name}->{data}}, $line);
	  }

	my $j    = 0;
	foreach (@{$self->{$name}->{data}})
	  {
	    my @data = split(/\s+/,$_);

	    foreach (@data)
	      {
		if ($_ ne "")
		  {	
		    my $value = $stress_factor * $_;
		    $j++;
		    $self->{$name}->{values}->{$j} = $value;
		    if ($value    > $self->{maximum_stress})
		      {
			$self->{maximum_stress} = $value;
			$self->{maximum_ftype}  = $name;
			$self->{maximum_plotn}  = $i;
		      }
		    if ($value    < $self->{minimum_stress})
		      {
			$self->{minimum_stress} = $value;
			$self->{minimum_ftype}  = $name;
			$self->{minimum_plotn}  = $i;
		      }
		  }
	      }
	  }

	$sum = $sum + $valid_flight * $blocks;
	$z   = $i;
      }

    $self->{VALID_FOR} = $sum;
    print STDERR " * DSG: $sum  TFlights: $z\n\n";
    #print STDERR "BLOCKs:  \n @{$self->{names}} \n";
  }


sub load_flight_type_sequence_data
  {
    my $self  = shift;
    my $fts_sequence_file = $self->{fts_file};  # The orig Class file!
	
	if ($self->{use_ft_sequence_in_sth_as_is} > 0)
	   {
	      for(1 .. $self->{use_ft_sequence_in_sth_as_is}) { push(@{$self->{sequence_array}}, $_); } 
		  return;
	   }

    open(INPUT, "< $fts_sequence_file" );

    my $j         =  0;
    my @big_array = ();
    
    while (<INPUT>)
      {
	$j++;
	my $sequence_line     = $_;
	chop($sequence_line);
	 next if $sequence_line =~ m/^\#/;
	$sequence_line        =~ s/^\s*//;

	my @array = split('\s+', $sequence_line);
	
        my @flight = split('_',$array[1]);
	push(@big_array, $flight[1]);
      }

    @{$self->{sequence_array}} = @big_array;
    close(INPUT);
  }

###################### Yes an STH file format in Sequence (instead of sigma format)  is also Possible ######################

sub write_STH_file_in_sequence   # not used by Default!!!!!!!!!!!!!!!
  {
    my $self          = shift;
    my $j             = 0;
    my $block         = 10;
    my $validity      = 1;

    open( STHOUT, "> $self->{sigma_file}" );
    
    foreach(@{$self->{header}})  {print STHOUT "$_\n";}

    foreach (@{$self->{sequence_array}})
      {
	my $ftype_num     = $_;
	my $name          = $self->{SEQ}->{$ftype_num}->{name_number};
	die unless (defined $name);  # please check sequence file!
	my @values;
	my $valid_flights = $self->{$name}->{valid};
	my $block         = $self->{$name}->{block};
	my $stress_points = $self->{$name}->{steps};
	my $i             = 0;
	$j                = $j + 1;

    print STHOUT sprintf("%10.2f%10.2f %s",     "$validity","$block","\n");
    print STHOUT sprintf("%10i%62s%-10s%5s%s",  "$stress_points","","$name", "$j","\n");
    print STDERR " $name  $j\n";
                
	for (1 .. $stress_points)
	  {
	    $i  = $_;

	    push(@values, $self->{$name}->{values}->{$i});
	  }

	$i = 0;

	foreach (@values)
	  {
	    my $value = $_;
	    $i++;
	    print STHOUT sprintf("%10.2f", "$value");

	    if ($i == 8)
	      {
		$i  = 0;
		print STHOUT "\n";	
	      }
	  }

	unless ($i == 0)
	  {
	    $i  = 0;
	    print STHOUT "\n"; 
	  }

	#print STHOUT "\n"; # extra line
      }

    close(STHOUT);
  }


sub add_safe_material_7010_header
  {
    my $self = shift;
    my $sigma_file    = $self->{sigma_file};

	$self->{material_header} = << "end_material_header";
.DEL.*
ABRETIT '%CHAINE'  'SEQUENCE TITLE'
ABRECOM '%COMMENT' 'A350-RIBs-LR-MISSION'
.TIT "''%COMMENT'"
 
! =================================
ABRESPE '%CHAINE'  'complexe'
ABRE    '%VERSION' '2' ! Model version
 
ABRETIT '%CHAINE' 'FILTERING DATA'
ABREMOD '%VALFILT' '0.' ! FILTERING PARAMETER VALUE
ABREMTG '%GENFILT' 'NO' ! FILTERED SPECTRUM FILE STORAGE/YES/NO
ABREMOD '%NOMFIC' 'A380-RiBs' ! FILTERED SPECTRUM FILE NAME
 
ABRETIT '%CHAINE'  'GENERAL DATA'
ABREMTG '%CALCUL' \'$self->{analysis_type}\' ! CALCULATION TYPE/initiation/propagation/initiation+propagation/no calculation
ABREMOD '%XSIG' '1' ! STRESS FACTOR
ABREMTG '%NIVEAU' 'NO' ! LEVEL FILE .papniv GENERATION/YES/NO
 
ABRETIT '%CHAINE' 'MATERIAL'
ABREMOD '%NOMMAT' \'$self->{use_this_material}\' ! MATERIAL NAME
 
ABRETIT '%CHAINE' 'CRACK INITIATION PARAMETERS'
ABREMTG '%LOI' 'Fatigue Method Manual' ! BEHAVIOUR MODEL/Fatigue Method Manual/Haigh Diagram/Old Manual Law
ABREMOD '%IQF' '140' ! IQF COEFFICIENT (MPa)
ABREAID '%CHAINE' 'KSNUL=0 : cycles suppressed if Smax<0 and Smin=0 if Smin<0'
ABREAID '%CHAINE' 'KSNUL=1 : cycles suppressed if Smax<0'
ABREAID '%CHAINE' 'KSNUL=2 : cycles unchanged'
ABREMTG '%KSNUL' 'KSNUL=1' ! CYCLE ELIMINATION RULE/KSNUL=0/KSNUL=1/KSNUL=2
 
ABRETIT '%CHAINE' 'CRACK PROPAGATION PARAMETERS'
ABREMTG '%COMPRESSION' 'YES' ! COMPRESSION CONSIDERED/YES/NO
 
ABRETIT '%CHAINE' 'FLIGHT DATA'
ABREMOD '%NBVOLCAL' '-1' ! NB OF FLIGHTS CONSIDERED IN CALCULATION (ALL = -1)
 
ABREMOD '%VE1' '1' ! EDITABLE FLIGHT NUMBER 1
ABREMOD '%VE2' '2' ! EDITABLE FLIGHT NUMBER 2
ABREMOD '%VE3' '3' ! EDITABLE FLIGHT NUMBER 3
ABREMOD '%VE4' '4' ! EDITABLE FLIGHT NUMBER 4
ABREMOD '%VE5' '5' ! EDITABLE FLIGHT NUMBER 5
 
!*******************************************************************************
!*******************************************************************************
!*******************************************************************************
!*******************************************************************************
!*******************************************************************************
!*******************************************************************************
!*******************************************************************************
!*******************************************************************************
!*******************************************************************************
!*******************************************************************************
!*******************************************************************************
!*******************************************************************************
!*******************************************************************************
!*******************************************************************************
!*******************************************************************************
!*******************************************************************************
!*******************************************************************************
!*******************************************************************************
!*******************************************************************************
!*******************************************************************************
!*******************************************************************************
!*******************************************************************************
!*******************************************************************************
!*********************************************************
! FIN_EN_TETE_UTILE_FICHIER_SIGMA
 
! Copyright SAMTECH - ASAV ............................. VERSION  9.1-05
! Responsable:       Librairies: Dev\\ Off\\ Date: 27-05-02
!
! Historique de la routine (creation,modification,correction)
! +--------------------------------------------------------------------+
! !Programmeur! Commentaires                          ! Date   !Version!
! +-----------!---------------------------------------!--------!-------+
! ! Mozar     ! creation                              !12-12-00! 9.0-06!
! ! Mozar     ! limite a 3600 caracteres ds entete    !04-01-01! 9.0-06!
! ! klein     ! version anglaise                      !16-01-01! 9.0-06!
! ! robert    ! abreTOg --> abreMTg                   !16-01-01! 9.0-06!
! ! Mozar     ! add var pour repertoire pertubations  !14-03-01! 9.0-07!
! ! robert    ! modif libelles                        !30-05-01! 9.0-08!
! ! Mozar     ! add colonne unite de reference        !12-07-01! 9.0-09!
! ! klein     ! modif commentaire col refer unit      !30-08-01! 9.0-09!
! ! klein     ! compression considered: no -> yes     !25-09-01! 9.1-02!
! ! klein     ! corr nb carac avant FIN_EN_TETE = 3600!27-05-02! 9.1-05!
! +--------------------------------------------------------------------+
!
! **********************************************************************
	
end_material_header
    
	 open( SIGMAOUT, "> $self->{sigma_file}" );	
	 my @mat = split('\n', $self->{material_header});

     foreach (@mat) 
       {
	     my $line = $_;
	     chop($line) if ($line =~m/"\n"$/);
	     print SIGMAOUT "$line\n";
       }
     close(SIGMAOUT);
  }

# end of program.


sub add_material_header   # not used by Default!!!!!!!!!!!!!!!
  {
    my $self = shift;
    my $mat_file      = $self->{mat_file} = 'safe_7010_header.log';
    my $sigma_file    = $self->{sigma_file};

    open(MAT,  "<". $self->{mat_file});
    my @mat       = <MAT>;
    chop(@mat);
    close(MAT);

    open( SIGMAOUT, "> $self->{sigma_file}" );

    foreach (@mat) 
      {
	print SIGMAOUT "$_\n";
      }
    close(SIGMAOUT);
  }



sub activate_objscan
  {
    my $self  = shift;

    my $mw = MainWindow->new();
    $mw->title("Data and Object Scanner");
    my $scanner   = $mw->ObjScanner(
				    caller => $self
				   )->pack(
					   -expand => 1
					  );
  }


#############################################################################
