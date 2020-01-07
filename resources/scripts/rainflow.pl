#!/usr/local/bin/perl -w

use strict;
use POSIX qw(log10 ceil floor);

my $self                    = {};
my $root                    = $self->{ROOT};

$self->{input}              = $ARGV[0];
$self->{output_file_format} = $ARGV[1];

&start($self);

sub start
{
  my $self   = shift;
  unless (-e  "$self->{input}")
  {
    print STDERR " *** STH file not found!\n";
    return;
  }
  
  $self->{output} = $self->{input} . '.rflow';
  &rainflow_sth_file_risfor_format($self);
}

sub rainflow_sth_file_risfor_format
  {
    my $self   = shift;
    my $file   = $self->{now_use_this_sth_file} = $self->{input};

    print STDERR " * begin rainflow for: $file \n";

    &load_all_sth_data($self);
    &organize_Markovmatrix($self);

    print STDERR " * ... end rainflow\n";
  }

sub organize_Markovmatrix
  {
    my $self          = shift;
    $self->{nmax}     = 0;
    my @double        = ();

    open( OUTPUT, "> $self->{output}" );

    #print OUTPUT "max: $self->{maximum_stress}   min: $self->{minimum_stress}\n";
    #print OUTPUT "        validfor     \=  $self->{VALID_FOR}\n";
    #print OUTPUT "begin $self->{output_file_format} RAINFLOW \n";

    my $max_stress = $self->{maximum_stress};
    my $min_stress = $self->{minimum_stress};
    my $validfor   = $self->{VALID_FOR};

    for (1 .. 64)
      {
	my $num  = $_;

	for (1 .. 64)
	  {
	    $double[$num][$_]    = 0;
	  }
      }

    $self->{rain}->{dg} = ($self->{maximum_stress} - $self->{minimum_stress}) / 64.0;
    $self->{rain}->{dg} = sprintf("%10.4f","$self->{rain}->{dg}");

    $self->{gr}->{1}    = sprintf("%10.4f","$self->{minimum_stress}");

    for (2 .. 65)
      {
	my $num = $_ - 1;
	$self->{gr}->{$_} = $self->{gr}->{$num} + $self->{rain}->{dg};
	$self->{gr}->{$_} = sprintf("%10.4f","$self->{gr}->{$_}");
      }

    foreach (@{$self->{names}})
      {
	my $name          = $_;
	my @values        = ();
	my $valid_flights = $self->{$name}->{valid};
	my $block         = $self->{$name}->{block};
	my $stress_points = $self->{$name}->{steps};

	for (1 .. $stress_points)
	  {
	    push(@values, $self->{$name}->{values}->{$_});
	  }

 	@{$self->{xyz}} = @values;

	&call_rainflow_for_job($self, $name);

	my @double2 = @{$self->{double2}};

	print STDERR "  * processing $name ... \n";

	for (1 .. 64)
	  {
	    my $num  = $_;

	    for (1 .. 64)
	      {
	       #print OUTPUT " double $num $_ $double[$num][$_] $double2[$num][$_]\n";
		$double[$num][$_] = $double[$num][$_] + ($double2[$num][$_] * $valid_flights * $block);
	       #$double[$num][$_] = sprintf("%10i","$double[$num][$_]")
	      }
	  }
      }

    #########################################################################################

    for (1 .. 64)
      {
	my $no   = $_;
	my $num  = $_ + 1;

	for ($num .. 64)
	  {
	    if (($double[$no][$_] == 0) && ($double[$_][$no] == 0))
	      {
		goto LABEL500;
	      }

	    if ($double[$no][$_] == $double[$_][$no])                          # Symmetric Matrix - Case 1
	      {
		$self->{nmax}    =  $double[$no][$_];
		#print OUTPUT " Case 1: $no $_ $self->{nmax}\n";
	      }
	    elsif (($double[$no][$_] != $double[$_][$no]) && 
 		   ($double[$no][$_] != 0) &&
		   ($double[$_][$no] != 0))                                    # Assymmetric Matrix - not zero - Case 2
	      {	
		&minimum_value($self, $double[$no][$_],$double[$_][$no]);
		&maximum_value($self, $double[$no][$_],$double[$_][$no]);
		my $min = $self->{extra}->{min};
		my $max = $self->{extra}->{max};

		$self->{nmax}  = $min + 0.5 * ($max - $min);
		#print OUTPUT " Case 2: $no $_ $self->{nmax}\n";
	      }
	    elsif ((($double[$no][$_] != 0) && ($double[$_][$no] == 0)) ||
		   (($double[$_][$no] != 0) && ($double[$no][$_] == 0)))       # Assymmetric Matrix - with zero - Case 3
	      {
		&maximum_value($self, $double[$no][$_],$double[$_][$no]);
		my $max = $self->{extra}->{max};
		$self->{nmax}  = 0.5 *  $max;
		#print OUTPUT " Case 3: $no $_ $self->{nmax}\n";
	      }
	    else 
	      {
		print STDERR "  *** nomatch  $double[$no][$_]  $double[$_][$no] \n";
	      }

	    &minimum_value($self, $no, $_);
	    &maximum_value($self, $no, $_);

	   # print STDERR "$no $_ $self->{nmax} \n";

	    my $imin           = $self->{extra}->{min};
	    my $imax           = $self->{extra}->{max};
	    $self->{sfmax}     = $self->{gr}->{$imax} + $self->{rain}->{dg};
	    $self->{sfmin}     = $self->{gr}->{$imin};
	    $self->{rrr}       = $self->{sfmin} / $self->{sfmax};
	    $self->{rnmax}     = $self->{nmax};

	    if (($self->{rrr} > 1.0) && ($self->{sfmax} < 0))
	      {
		goto LABEL500;
	      }
	    else 
	      {
		if ($self->{output_file_format} =~m/STPNL/)
		  {
		    print OUTPUT sprintf("%10i%10.1f%10.2f%10.2f %s","0","$self->{rnmax}","$self->{sfmax}","$self->{rrr}","\n");
		  }
		elsif ($self->{output_file_format} =~m/DATIG/)
		  {
		    print OUTPUT sprintf("%10s%10.1f%10.2f%10.2f %s"," ","$self->{rnmax}","$self->{sfmax}","$self->{sfmin}","\n");
		  }

		print STDERR sprintf("%10.1f%10.2f%10.2f%10.2f %s",
				     "$self->{rnmax}","$self->{sfmax}","$self->{sfmin}","$self->{rrr}","\n");
	      }

	  LABEL500:
	  }
      }

    print OUTPUT sprintf("%10i%10.1f%s","1","$self->{VALID_FOR}","\n");

    foreach (@{$self->{header}})
      {
	print OUTPUT "$_\n";
      }

    close(OUTPUT);
    @{$self->{double}} = @double;
  }

sub maximum_value
  {
    my $self  = shift;
    my @array = @_;
    my $i     = 0;

    $self->{extra}->{max} = -1000000000;

    foreach (@array)
      {
	my $value    = $_;
	if ($value ne "")
	  {
	    $i++;
	    if ($value  > $self->{extra}->{max})
	      {
		$self->{extra}->{max} = $value;
	      }
	  }
      }

    if ($i == 0) 
      {
	print STDERR " ***  array was empty! \n";
	die;
      }
   #print STDERR " *   max |@array| is  $self->{extra}->{max}\n";
  }


sub minimum_value
  {
    my $self  = shift;
    my @array = @_;
    my $i     = 0;

    $self->{extra}->{min} = 1000000000;

    foreach (@array)
      {
	my $value    = $_;
	if ($value ne "")
	  {
	    $i++;
	    if ($value  < $self->{extra}->{min})
	      {
		$self->{extra}->{min} = $value;
	      }
	  }
      }

    if ($i == 0) 
      {
	print STDERR " ***  array was empty! \n";
	die;
      }
   #print STDERR " *   min |@array| is  $self->{extra}->{min}\n";
  }

sub call_rainflow_for_job
  {
    my $self          = shift;
    my $name          = shift;

    my $accuracy      = 0.01;
    my $stress_points = $self->{$name}->{steps};
    my $valid_flights = $self->{$name}->{valid};
    my $block         = $self->{$name}->{block};
    $self->{ip1}      = 0;
    $self->{ip2}      = 0;

    my @array         = @{$self->{xyz}};
    my @double        = ();
    my @s             = ();

    for (1 .. 64)
      {
	my $num  = $_;

	for (1 .. 64)
	  {
	    $double[$num][$_]    = 0;
	  }
      }

    unshift(@array, '0');

    $s[1]   = $array[1];  #    shift(@array);

    my $p   = 1;
    my $q   = 1;
    my $z   = 0;
    my $f   = 0;

  LABEL200:

    $p      = $p + 1;
    $q      = $q + 1;
    $z      = $q - 1;
    $s[$p]  = $array[$q];

    if ($q == $stress_points)
      {
	$f = 1;
      }

  LABEL300:

    if ($p >= 4)
	  {
	    if (($s[$p-2] > $s[$p-3]) && ($s[$p-1] >= $s[$p-3]) && ($s[$p] >= $s[$p-2]))
	      {
		goto LABEL400;
	      }

	    if (($s[$p-2] < $s[$p-3]) && ($s[$p-1] <= $s[$p-3]) && ($s[$p] <= $s[$p-2]))
	      {
		goto LABEL400;
	      }
	  }

    if ($f == 0)
      {
	goto LABEL200;
      }

    goto LABEL1000;

  LABEL400:

    for (2 .. 65)
      {
	my $num1 =  $_;
	my $diff = abs($s[$p-1] - $self->{gr}->{$num1});
	
	    if (($s[$p-1] <= $self->{gr}->{$num1}) || ($diff <= $accuracy))
	      {
		$self->{ip1} = $num1 - 1;
		
		goto LABEL415;
	      }
      }

  LABEL415:

    for (2 .. 65)
      {
	my $num1 =  $_;
	my $diff = abs($s[$p-2] - $self->{gr}->{$num1});
	
	    if (($s[$p-2] <= $self->{gr}->{$num1}) || ($diff <= $accuracy))
	      {
		$self->{ip2} = $num1 - 1;
		
		goto LABEL425;
	      }
      }

  LABEL425:

    $double[$self->{ip2}][$self->{ip1}] = $double[$self->{ip2}][$self->{ip1}] + 1;
    $double[$self->{ip1}][$self->{ip2}] = $double[$self->{ip1}][$self->{ip2}] + 1;

    if (($self->{ip1} == 0) || ($self->{ip2} == 0))  {print STDERR "  *   what to do \?\n"; }

    $s[$p-2] =  $s[$p];
    $p = $p - 2;

    #print STDERR " $p    $q     $double[$self->{ip2}][$self->{ip1}]           $double[$self->{ip1}][$self->{ip2}] \n";

    goto LABEL300;


  LABEL1000:
  #  print STDERR "P: $p     Q: $q\n";

    $q = 0;

  LABEL1100:

    $q = $q +1;

    if ($q == $p) 
      {
	goto LABEL3000;
      }

    for (2 .. 65)
      {
	my $num1 =  $_;
	my $diff = abs($s[$q+1] - $self->{gr}->{$num1});
	
	if (($s[$q+1] <= $self->{gr}->{$num1}) || ($diff <= $accuracy))
	  {
	    $self->{ip2} = $num1 - 1;
	    goto LABEL1115;
	  }
      }

  LABEL1115:

    for (2 .. 65)
      {
	my $num1 = $_;
	my $diff = abs($s[$q] - $self->{gr}->{$num1});
	if (($s[$q] <= $self->{gr}->{$num1}) || ($diff <= $accuracy))
	  {
	    $self->{ip1} = $num1 - 1;
	    goto LABEL1125;
	  }
      }

  LABEL1125:

    $double[$self->{ip2}][$self->{ip1}] = $double[$self->{ip2}][$self->{ip1}] + 1;

    goto LABEL1100;

  LABEL3000:

    for (1 .. 64)          # matrix diagonal to zero
      {
	$double[$_][$_]  = 0;
      }

    @{$self->{double2}} = @double;

    #die;
  }

sub load_all_sth_data
  {
    my $self   = shift;
    my $stress_factor = 1.0;
    my $column = 8.0;
    my $sum    = 0.0;
    my $sth    = $self->{now_use_this_sth_file};

    $self->{maximum_stress} = -1000000;
    $self->{minimum_stress} =  1000000;

    @{$self->{header}} = ();
    @{$self->{names}} = ();

    open( INPUT, "< $sth" );
    my @contents    = <INPUT>;
    chop(@contents);
    close(INPUT);

    foreach (0 .. 3) 
      {
	my $line = shift(@contents);
	push(@{$self->{header}}, $line);
      }

    for(1 .. 100001)                     # Max FT number expected.
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
       #$name               = 'TF_' . $i  unless (defined $name);
	$name               = 'TFLIGHT_' . $i;
	$name               =~s/\s*//;

	push(@{$self->{names}}, $name);
	$self->{$name}->{steps} = $steps;
	$self->{$name}->{valid} = $valid_flight;
	$self->{$name}->{block} = $blocks;
	
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
      }

    $self->{VALID_FOR} = $sum;
    print STDERR "BLOCKs:  \n @{$self->{names}} \n";
  }

# end of program.

1;

