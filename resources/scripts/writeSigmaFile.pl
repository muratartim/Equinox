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
$self->{use_this_validity} = $ARGV[2];
$self->{analysis_type} = $ARGV[3];
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

#    print SIGMAOUT sprintf("%5s%7i ! TOTAL NUMBER OF FLIGHTS %s", "NBVOL","$mission_validity","\n");

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

	print SIGMAOUT sprintf("%6s%10i%s", "VOL NO","$j","\n");
	print SIGMAOUT sprintf("%17s%s", "NOMBRE DE VALEURS","\n");
	print SIGMAOUT sprintf("%22i%s", "$stress_points","\n");
	print SIGMAOUT sprintf("%16s%s", "SUITE DE VALEURS","\n");
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
	    print SIGMAOUT sprintf("%11.5f", "$value");

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

#	print SIGMAOUT "\n";
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
ABRE    "%SPECTYP" "SPECTRE COMPLEXE"
ABREFAT "%CHAINE" "/home/edfat/MODELE/SPECTRE/spectre-complexe.sigma"
ABREMOT "%TITRE" "          PS.  116TOIT-FUSELAGE - BAE(SAFE spectrum - ESADT)" ! TITRE DU VOL
ABRE    "%NBVOL"  \"$self->{use_this_validity}\"
ABRETIT "%CHAINE" "DONNEES FILTRAGE"
ABREMOD "%OMEGA" "0." ! VALEUR DU PARAMETRE OMEGA
ABRETOG "%STOC"  "PAS DE STOCKAGE" ! FICHIER SIGMA FILTRE/STOCKAGE/PAS DE STOCKAGE
ABRETIT "%CHAINE" "DONNEES AMORCAGE+PROPAGATION"
ABRETOG "%TYPE" \"$self->{analysis_type}\" ! TYPE DE CALCUL/PAS DE CALCUL/AMORCAGE/PROPAGATION/AMORCAGE+PROPAGATION
ABREMOD "%XM" "1."         ! COEFFICIENT MULTIPLICATIF DES CONTRAINTES
ABREMOD "%NOMMAT" "material.mat" ! DESIGNATION MATERIAU
ABRETOG "%LOI" "MANUEL"     ! LOI DE COMPORTEMENT/RESEAU/MANUEL/ANCIENNE LOI
ABRETOG "%KSNUL" "Smax<0"   ! ELIMINATION DES CYCLES COMPRESSION/Smax<0-Smin=0 SI R<0/Smax<0/PAS D'ELIMINATION/
ABREMOD "%IQF" "100."   ! INDICE DE QUALITE EN FATIGUE (MPa)
ABRETOG "%PREF" "COMPRESSION" ! PREFFAS/COMPRESSION/PAS DE COMPRESSION
ABRETOG "%PAP" "PAS DE GENERATION" ! FICHIER NIVEAUX/GENERATION/PAS DE GENERATION
ABREMOD "%VOL1" "1"     ! VOL EDITABLE NUMERO 1
ABREMOD "%VOL2" "0"     ! VOL EDITABLE NUMERO 2
ABREMOD "%VOL3" "0"     ! VOL EDITABLE NUMERO 3
ABREMOD "%VOL4" "0"     ! VOL EDITABLE NUMERO 4
ABREMOD "%VOL5" "0"     ! VOL EDITABLE NUMERO 5
ABREFIN	
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



#############################################################################
