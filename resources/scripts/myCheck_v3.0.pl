#! /opt/perl5/bin/perl -w

use strict;
use POSIX qw(sin cos ceil floor log10 atan);
use Tk; 
use Tk::COUNT_ANA_STATISTICS_080;
use Tk::GENERATE_STH_PROFILE_076;
use Tk::COMPARE_XLSTABLE_TXT_014;
use Tk::PLOT_MAXMEANMIN_FSB_040;
use Tk::PLOT_DELTAP_STH_HQXP_018;
use Tk::PLOT_STH_MAX_MIN_HQ_029;
use Tk::COMPARE_TXT_MX_vs_SML_CLASS_ISSY_024;
use Tk::MyCHECK_BASIC_EDITOR_010;
use Tk::ObjScanner;

my $root                         = {};
$root->{global_inputfilename}    = 'HQ_GLOBAL_INPUT_FILE.hqx';

if (defined $ARGV[0])  {$root->{global_inputfilename}    = $$ARGV[0]; print STDERR "using inputfile: $ARGV[0]\n";}
$root->{number_of_missions}                = 0;
$root->{GLOBAL}->{activate_text_editor}    = 1;
print STDERR "  * using Global Inputs from |$root->{global_inputfilename}| \n";
&load_hqx_global_input_information($root);
$root->{GLOBAL}->{GLOBAL_LOGFILE_NAME} = $root->{GLOBAL}->{GLOBAL_LOGFILE};
@{$root->{log_files}} = ();


if (defined $root->{COMPARE_MX_SML})
  {
 	 print STDERR "  * initializing Checks: TXT  MiX vs. SinGle  .....\n";
     $root->{COMPARE_TXT_MX_vs_SML_CLASS_ISSY_024} = Tk::COMPARE_TXT_MX_vs_SML_CLASS_ISSY_024->new(ROOT => $root,);  
     $root->{COMPARE_TXT_MX_vs_SML_CLASS_ISSY_024} = {};			 
  }


for (1 .. $root->{number_of_missions})
  {
     my $m_num    = $_;
	 $root->{GLOBAL}->{GLOBAL_LOGFILE} = $root->{GLOBAL}->{GLOBAL_LOGFILE_NAME};
     $root->{GLOBAL}->{GLOBAL_LOGFILE} =~s/\.log\s*$//;	 
	 $root->{GLOBAL}->{GLOBAL_LOGFILE} = $root->{GLOBAL}->{GLOBAL_LOGFILE} . '_'. $m_num . '.log';
	 push(@{$root->{log_files}}, $root->{GLOBAL}->{GLOBAL_LOGFILE});
     open (GLOBAL_LOG, ">". $root->{GLOBAL}->{GLOBAL_LOGFILE}) or print STDERR " *** Cannot Create GLOBAL_LOG |$root->{global_inputfilename}|\n";
     print GLOBAL_LOG "\n\n\n\n%%%%%%%%%%%%%%%%% Begin Process for Mission: $_  %%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n\n";
     close(GLOBAL_LOG);
	 print STDERR "  * initializing actions for Mission: $root->{MISSION}->{$m_num}->{MISSION}\n";
	 print STDERR "  * initializing Checks: ConvTable vs TXT  .....\n";
     $root->{COMPARE_XLSTABLE_TXT_014} = Tk::COMPARE_XLSTABLE_TXT_014->new(  # Compare EXCEL with TXT         <1>
                                                                           ROOT       => $root,
                                                                           MY_MISSION => $m_num,
                                                                          ); 
     $root->{COMPARE_XLSTABLE_TXT_014} = {};																		  
     print STDERR "  * initializing Checks: Count Statistics  .....\n";																		  
     $root->{COUNT_ANA_STATISTICS_080} = Tk::COUNT_ANA_STATISTICS_080->new(   # Count Statistics              <2>
                                                                           ROOT       => $root,
                                                                           MY_MISSION => $m_num,
                                                                          );
	 $root->{COUNT_ANA_STATISTICS_080} = {};																	  
	 print STDERR "  * initializing Checks: Generate Profile  .....\n";																	  
     $root->{GENERATE_STH_PROFILE_076} = Tk::GENERATE_STH_PROFILE_076->new(   # Generate Profile & Check ISAMI compatibility       <3>
                                                                           ROOT       => $root,
                                                                           MY_MISSION => $m_num,
                                                                          );
	 $root->{GENERATE_STH_PROFILE_076} = {};																	  
	 print STDERR "  * initializing Checks: Plot     Profile  .....\n";																	  
     $root->{PLOT_MAXMEANMIN_FSB_040} =  Tk::PLOT_MAXMEANMIN_FSB_040->new(    # Plot PROFILE                  <4>
                                                                           ROOT       => $root,
                                                                           MY_MISSION => $m_num,
																		   PLOT       => 'PROFILE',
                                                                          );
	$root->{PLOT_MAXMEANMIN_FSB_040} = {};																	  
	 print STDERR "  * initializing Checks: Plot     Sequence .....\n";	 																		  
     $root->{PLOT_DELTAP_STH_HQXP_018} = Tk::PLOT_DELTAP_STH_HQXP_018->new(   # Plot STH                      <5a>
                                                                           ROOT       => $root,
                                                                           MY_MISSION => $m_num,
																		   PLOT       => 'STH',
                                                                          ); 
     $root->{PLOT_DELTAP_STH_HQXP_018} = {};																	  
     print STDERR "  * initializing Checks: Plot     DeltaP   .....\n";																		  
     $root->{PLOT_DELTAP_STH_HQXP_018} = Tk::PLOT_DELTAP_STH_HQXP_018->new(   # Plot DELTAP                   <5b>
                                                                           ROOT       => $root,
                                                                           MY_MISSION => $m_num,
																		   PLOT       => 'PLOT',
                                                                          ); 
     $root->{PLOT_DELTAP_STH_HQXP_018} = {};
	 # NEW v1.8
     print STDERR "  * initializing Checks: Plot STH Max Min  .....\n";																		  
     $root->{PLOT_STH_MAX_MIN_HQ_029} = Tk::PLOT_STH_MAX_MIN_HQ_029->new(   # Plot STH MAX MIN                <6>
                                                                           ROOT       => $root,
                                                                           MY_MISSION => $m_num,
																		   PLOT       => 'STH',
                                                                          ); 	 
	 $root->{PLOT_STH_MAX_MIN_HQ_029} = {};
	 
     open (GLOBAL_LOG, ">>". $root->{GLOBAL}->{GLOBAL_LOGFILE});
     print GLOBAL_LOG "  * End of HQX process!"; 
     close(GLOBAL_LOG);	 
  }
print STDERR "   * End of HQX process!"; 

if ($root->{GLOBAL}->{activate_text_editor} > 0) 
  {
    $root->{MyCHECK_BASIC_EDITOR_010} = Tk::MyCHECK_BASIC_EDITOR_010->new(ROOT  => $root,);
    MainLoop;
  }
  
#&activate_objscan($root);



sub activate_objscan
  {
    my $root  = shift;
    my $mw    = MainWindow->new();
    $mw->title("Data and Object Scanner");
    my $scanner   = $mw->ObjScanner(caller => $root,)->pack(-expand => 1,);
  }



  

sub load_hqx_global_input_information
  {
    my $root = shift;
	
	open(INPUT,  "<". $root->{global_inputfilename}) or print STDERR " *** Inputfile not Found |$root->{global_inputfilename}|\n";
    my @input = <INPUT>;
	chop(@input);
	close(INPUT);
	
	my $type                    = 'NA';    
	$root->{number_of_missions} = 0;
	foreach (@input)
	  {
		if (($_ =~m/^\s*$/) || ($_ =~m/^\s*\#/)) {next;}
    
		if ($_ =~m/^\s*BEGIN_GLOBAL/i)               {$type = 'GLOBAL';        next;} #
		if ($_ =~m/^\s*END/i)                        {$type = 'NA';            next;} #
		if ($_ =~m/^\s*BEGIN_MISSION/i)              {$type = 'MISSION'; $root->{number_of_missions}++; next;} #
		if ($_ =~m/^\s*BEGIN_OPTIONS_COUNT_ANA/i)    {$type = 'COUNT_ANA';     next;} #	
		if ($_ =~m/^\s*BEGIN_OPTIONS_GENERATE_STH/i) {$type = 'GENERATE_STH';  next;} #	
		if ($_ =~m/^\s*BEGIN_OPTIONS_PLOTS/i)        {$type = 'PLOT_OPTIONS';  next;} #	
		if ($_ =~m/^\s*BEGIN_THIS_AC_4_DIGIT_ROT_MLG_CODES/i) {$type = 'ROT_MLG_CODES';  next;}	
		if ($_ =~m/^\s*BEGIN_COMPARE_TXT/i)          {$type = 'COMPARE_MX_SML';  next;}	
		
    	if ($type eq 'GLOBAL') 
	       {
    	      my ($key, $value) = split('\s*=\s*', $_);
		      if((defined $key) && (defined $value))
		         {
		            $key   =~s/\s*//g;
					if ($value =~m/#/) {$value =~s/^\s*//; my @a = split('#', $value); $value = $a[0]; $value =~s/\s*//g;} else {$value =~s/^\s*//; $value =~s/\s*$//;} 
					$root->{GLOBAL}->{$key}  = $value;  unless ((defined $key) && (defined $value)) {print STDERR " *** Undefined Paired Values |$key|$value|\n";}
		         }		  
	       }	   
    	if ($type eq 'MISSION') 
	       {
    	      my ($key, $value) = split('\s*=\s*', $_);
		      if((defined $key) && (defined $value))
		         {
		            $key   =~s/\s*//g;
					if ($value =~m/#/) 
					    {
						   $value =~s/^\s*//; 
						   my @a = split('#', $value); 
						   $value = $a[0]; $value =~s/^\s*//; $value =~s/\s*$//;
						} 
					else 
					    {
						   $value =~s/^\s*//; $value =~s/\s*$//;
						} 
					$root->{MISSION}->{$root->{number_of_missions}}->{$key} = $value;  unless ((defined $key) && (defined $value)) {print STDERR " *** Undefined Paired Values |$key|$value|\n";}
		         }		  
	       }
    	if ($type eq 'COUNT_ANA') 
	       {
    	      my ($key, $value) = split('\s*=\s*', $_);
		      if((defined $key) && (defined $value))
		         {
		            $key   =~s/\s*//g;
					if ($value =~m/#/) {$value =~s/^\s*//; my @a = split('#', $value); $value = $a[0]; $value =~s/\s*//g;} else {$value =~s/^\s*//; $value =~s/\s*$//;} 
					$root->{COUNT_ANA}->{$key} = $value;  unless ((defined $key) && (defined $value)) {print STDERR " *** Undefined Paired Values |$key|$value|\n";}
		         }		  
	       }
    	if ($type eq 'GENERATE_STH') 
	       {
    	      my ($key, $value) = split('\s*=\s*', $_);
		      if((defined $key) && (defined $value))
		         {
		            $key   =~s/\s*//g;
					if ($value =~m/#/) {$value =~s/^\s*//; my @a = split('#', $value); $value = $a[0]; $value =~s/\s*//g;} else {$value =~s/^\s*//; $value =~s/\s*$//;} 
					$root->{GENERATE_STH}->{$key} = $value;  unless ((defined $key) && (defined $value)) {print STDERR " *** Undefined Paired Values |$key|$value|\n";}
		         }		  
	       }
    	if ($type eq 'PLOT_OPTIONS') 
           {
	          my ($key, $value) = split('\s*=\s*', $_);
	          if((defined $key) && (defined $value))
	             {
		            $key   =~s/\s*//g;
					if ($value =~m/#/) {$value =~s/^\s*//; my @a = split('#', $value); $value = $a[0]; $value =~s/\s*//g;} else {$value =~s/^\s*//; $value =~s/\s*$//;} 
		            $root->{PLOT_OPTIONS}->{$key} = $value;  unless ((defined $key) && (defined $value)) {print STDERR " *** Undefined Paired Values |$key|$value|\n";}
	             }		  
           }
    	if ($type eq 'ROT_MLG_CODES') 
           {
	          my ($key, $value) = split('\s*=\s*', $_);
	          if((defined $key) && (defined $value))
	             {
		            $key   =~s/\s*//g;
					if ($value =~m/#/) {$value =~s/^\s*//; my @a = split('#', $value); $value = $a[0];} else {$value =~s/^\s*//;} 
		            $root->{ROT_MLG_CODES}->{$key} = $value;  unless ((defined $key) && (defined $value)) {print STDERR " *** Undefined Paired Values |$key|$value|\n";}
	             }		  
           }
    	if ($type eq 'COMPARE_MX_SML') 
           {
	          my ($key, $value) = split('\s*=\s*', $_);
	          if((defined $key) && (defined $value))
	             {
		            $key   =~s/\s*//g;
					if ($value =~m/#/) {$value =~s/^\s*//; my @a = split('#', $value); $value = $a[0]; $value =~s/\s*//g;} else {$value =~s/^\s*//; $value =~s/\s*$//;} 
		            $root->{COMPARE_MX_SML}->{$key} = $value;  unless ((defined $key) && (defined $value)) {print STDERR " *** Undefined Paired Values |$key|$value|\n";}
	             }		  
           }		   
		   
      } # end loop
  } # end sub


