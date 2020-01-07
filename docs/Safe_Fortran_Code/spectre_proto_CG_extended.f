       PROGRAM CHARGE4
C
C ********
C FICHIERS 
C         prefixe.sigma      TAPE 2 
C         prefixe.dossier    TAPE 3 
C         prefixe.trace      TAPE 4 
C         prefixe.papniv     TAPE 7 
C         prefixe.dbmat      TAPE 8 
C         prefixe.traniv     TAPE 9 
C         prefixe-w???.sigma TAPE 10
C         prefixe.erreurs    TAPE 19
C **********************************
      PARAMETER(MAXSIG=800000,MAXCYC=MAXSIG/2,NBPAL=20,NBVOLM=100,
     &          MAXCAL=60,NRESX=5,NRESY=9,NHMAX=50)
C
      CHARACTER*80 NOMPB,TITRE,AMAT,NATURE,DESIG(NBPAL,NBVOLM)
      REAL SIG(MAXSIG),SRF(2,MAXCYC),XI(MAXSIG),SMOYRE(MAXCAL,NRESX),
     &     NRE(MAXCAL,NRESY),SALTRE(MAXCAL,NRESX,NRESY),
     &     CEQUI(NBPAL,NBVOLM),DELSI(NBPAL,NBVOLM),VALEUR(MAXCAL,100),
     &     SIGSIM(MAXSIG/1000,NBVOLM),HMA(NHMAX),HMI(NHMAX),HO(NHMAX)
      INTEGER NVOL(9),NPTREX(MAXCAL),NPTREY(MAXCAL),IFREQ(NBPAL,NBVOLM),
     &        ISEQ(NBVOLM,2),IEXIST(NBVOLM),NBPALR(NBVOLM),
     &        NPICS(NBVOLM),NFILT(NBVOLM)
C
      WRITE(6,*)
      WRITE(6,*) ':---------------------------:'
      WRITE(6,*) ': EXECUTION CHARGE-MODULE 2 :'
      WRITE(6,*) ':---------------------------:'
C
      DATA NECMIS,NIVMIN,NIVMAX/1E6,1E6,-1/
C
C LECTURE
C -------
      CALL OPENF(NOMPB)
      CALL LECDON(NOMPB,TITRE,AMAT,VALEUR,XM,RIQF,CE,A,B,C,XN,R02,RM,
     &            SIGMAD,SMOYRE,NRE,SALTRE,OMEGA,NPTREX,NPTREY,NVOL,
     &            NBVOL,ICAL,IPAP,ILOI,KSNUL,ITYPE,ICOMP,ISTOC)
      CALL INIEND(ENDTOT,SMINS,SMAXS,ENVOL,EFVOL,IMINS,IMAXS,ISEVS,
     &            ISEVP,NTOT)
C
      CALL ECRDST(NOMPB,TITRE,AMAT,SMOYRE,NRE,SALTRE,VALEUR,RIQF,OMEGA,
     &            CE,A,B,C,XN,R02,RM,SIGMAD,NPTREX,NPTREY,NBVOL,ILOI,
     &            KSNUL)
C
      IF(ITYPE.EQ.1) THEN
        CALL LECSIM(NOMPB,SIGSIM,ISEQ,IEXIST,NPICS,NVOL,NBSEQ,NBVOL)
        CALL ECRSIM(NOMPB,SIGSIM,ISEQ,IEXIST,NPICS,NBSEQ)
      ELSE IF(ITYPE.EQ.2) THEN
        CALL LECPAL(NOMPB,DESIG,CEQUI,DELSI,ISEQ,IFREQ,IEXIST,NBPALR,
     &              NBSEQ)
        CALL CALSIG(NOMPB,CEQUI,DELSI,IFREQ,ISEQ,IEXIST,NBPALR,NVOL,
     &              NBSEQ,NBVOL)
        CALL ECRPAL(NOMPB,DESIG,CEQUI,DELSI,ISEQ,IFREQ,IEXIST,NBPALR,
     &              NBSEQ)
      ENDIF
C
C FILTRAGE
C --------
      IU = 2
      IF(OMEGA.NE.0) THEN
        NTOTF = 0
        NTOT=0
        CALL OPENFI(NOMPB,OMEGA)
        CALL ECRIDE(NOMPB,10)
        DO I=1,NBVOL
          CALL LECSPE(NOMPB,I,XM,2,I,ILIG,SIG,XI,NPIC,NTOT)
          NAVANT = NPIC
          IF(NPIC.NE.0) THEN
            CALL CYCLE(NOMPB,SIG,OMEGA,SIGMAD,NPIC,I)
            IF(I.EQ.1) CALL TRANSI2(NOMPB,SIG,SIGN,OMEGA,SIGMAD,I,NPIC)
            IF(I.GE.2) CALL TRANSI1(NOMPB,SIG,SIGN,OMEGA,SIGMAD,I,NPIC)
            IF(I.EQ.NBVOL) CALL TRANSI3(NOMPB,SIG,SIG1,OMEGA,SIGMAD,I,
     &                                  NPIC)
            CALL VALIDE(NOMPB,SIG,NPIC)			
          ENDIF
          CALL STOCK1(NOMPB,SIG,NPIC,SIG1)
          CALL STOCKN(NOMPB,SIG,NPIC,SIGN)
          NFILT(I) = NAVANT-NPIC
          CALL ECRFIL(NOMPB,SIG,NFILT,0,I,NPIC,NTOTF)
        ENDDO
        IU = 10
        XM = 1.
      ENDIF
C
C RAIN-FLOW + MINER
C -----------------
      IF((ICAL.EQ.0).OR.(ICAL.EQ.1).OR.(ICAL.EQ.3)) THEN
        NTOT=0
        DO I=1,NBVOL
          CALL LECSPE(NOMPB,I,XM,IU,I,ILIG,SIG,XI,NPIC,NTOT)
          IF(NPIC.NE.0) THEN
            CALL ANALYS(SIG,NPIC,0,I,SMINS,SMAXS,SMAXV,SMINV,IMINS,
     &                  IMAXS,IMINV)
            CALL RMS(SIG,SMAXV,NPIC,0)
            IF((ICAL.EQ.1).OR.(ICAL.EQ.3)) THEN
              CALL RESTRU(SIG,NPIC,IMINV)
              CALL VALIDE(NOMPB,SIG,NPIC)			
              CALL RFLOW(SIG,NPIC,I,0,SRF,NCYCLE)
              IF(I.EQ.1) THEN
                ENVOLS = 0.
              ELSE
                ENVOLS = ENDTOT
              ENDIF
              CALL MINER(NOMPB,SRF,VALEUR,RIQF,SMOYRE,NRE,SALTRE,NPTREX,
     &                   NPTREY,NCYCLE,ILOI,KSNUL,0,I,ENDTOT)
              IF((ENDTOT-ENVOLS).GE.ENVOL) THEN
                ENVOL  = ENDTOT-ENVOLS
                ISEVS  = I
              ENDIF
            ENDIF
          ENDIF
        ENDDO
      ENDIF  
C
C PIC A PIC DESCENDANT + PREFFAS
C ------------------------------
      IF(((ICAL.EQ.2).OR.(ICAL.EQ.3)).OR.(IPAP.EQ.1)) THEN
        NTOT = 0
C
C CALCUL DU MIN-MAX SUR LA SEQUENCE
C ---------------------------------
        IF(ICAL.EQ.2) THEN
          NTOT = 0
          DO I=1,NBVOL
            CALL LECSPE(NOMPB,I,XM,IU,I,ILIG,SIG,XI,NPIC,NTOT)
            IF(NPIC.NE.0) THEN
              CALL ANALYS(SIG,NPIC,0,I,SMINS,SMAXS,SMAXV,SMINV,
     &                    IMINS,IMAXS,IMINV)
              CALL RMS(SIG,SMAXV,NPIC,0)
            ENDIF
          ENDDO  
        ENDIF    
        CALL INIPRE(SMINS,SMAXS,A,B,C,R02,RM,ICOMP,SMINSV,HMA,HMI,HO,
     &              VMICL,NCOM)
        NTOT = 0
        DO I=1,NBVOL
          CALL LECSPE(NOMPB,I,XM,IU,I,ILIG,SIG,XI,NPIC,NTOT)
          IF(NPIC.NE.0) THEN
            CALL PAPDES(SIG,NPIC,NBVOL,I,SDEB,SFIN)
            CALL CALPHA(SIG,SMAXS,NPIC,NBVOL,I,0,ALPHA,ALPHAS)
            IF(IPAP.EQ.1) CALL PAPNIV(NOMPB,TITRE,SIG,SMAXS,NPIC,NBVOL,   
     &      I,0,PMILLE,NIVMIN,NIVMAX,IRMIN,IRMAX,NECMIS,NVOLMI,
     &      NVOLMA)
            IF((ICAL.EQ.2).OR.(ICAL.EQ.3)) 
     &        CALL PREFAS(NOMPB,SIG,HMA,HMI,HO,VMICL,SMAXS,SMINSV,A,B,C,
     &                    XN,R02,RM,0,NPIC,ICOMP,0,EF,EFCL,NCOM)
          ENDIF
        ENDDO  
      ENDIF
C
C SECOND PASSAGE DE LA BOUCLE PREFFAS
C -----------------------------------
      IF((ICAL.EQ.2).OR.(ICAL.EQ.3)) THEN
        NTOT = 0
        DO I=1,NBVOL
          CALL LECSPE(NOMPB,I,XM,IU,I,ILIG,SIG,XI,NPIC,NTOT)
          IF(NPIC.NE.0) THEN
            CALL PAPDES(SIG,NPIC,NBVOL,I,SDEB,SFIN)
            IF((ICAL.EQ.2).OR.(ICAL.EQ.3)) THEN
              IF(I.EQ.1) THEN
                EFVOLS = 0.
              ELSE
                EFVOLS = EF 
              ENDIF
              CALL PREFAS(NOMPB,SIG,HMA,HMI,HO,VMICL,SMAXS,SMINSV,A,B,C,
     &                    XN,R02,RM,I,NPIC,ICOMP,2,EF,EFCL,NCOM)
              IF((EF-EFVOLS).GE.EFVOL) THEN
                EFVOL  = EF-EFVOLS
                ISEVP  = I
              ENDIF
            ENDIF
          ENDIF
        ENDDO
      ENDIF
C
C EDITION DES VOLS
C ----------------
      CALL EDITIO(NVOL,IMINS,IMAXS,ISEVS,ISEVP,K)
      DO I=1,K
        CALL LECSPE(NOMPB,NVOL(I),XM,IU,1,ILIG,SIG,XI,NPIC,IBID)
        IF(NPIC.NE.0) THEN
          XBID1=0.
          XBID2=0.
          CALL ANALYS(SIG,NPIC,1,NVOL(I),XBID1,XBID2,SMAXV,SMINV,IBID,
     &                IBID,IMINV)
          CALL RMS(SIG,SMAXV,NPIC,1)
          WRITE(NATURE,'(A4,I4)') 'vol-',NVOL(I)
          CALL DEBUT(NOMPB,NATURE,'temps','MPa (MPa)',XI,SIG,NPIC,4)
          IF((ICAL.EQ.1).OR.(ICAL.EQ.3)) THEN
            CALL RESTRU(SIG,NPIC,IMINV)
            CALL VALIDE(NOMPB,SIG,NPIC)			
            CALL RFLOW(SIG,NPIC,NVOL(I),1,SRF,NCYCLE)
            CALL ECRRF(NOMPB,SRF,4,NVOL(I),NCYCLE)
            XBID=0. 
            CALL MINER(NOMPB,SRF,VALEUR,RIQF,SMOYRE,NRE,SALTRE,NPTREX,
     &                 NPTREY,NCYCLE,ILOI,KSNUL,1,NVOL(I),XBID)
            IF((OMEGA.NE.0).AND.(ICAL.EQ.1)) 
     &        CALL EDIFIL(NFILT,1,NVOL(I))
          ENDIF
          IF((ICAL.EQ.2).OR.(ICAL.EQ.3).OR.(IPAP.EQ.1)) THEN
            CALL LECSPE(NOMPB,NVOL(I),XM,IU,1,ILIG,SIG,XI,NPIC,IBID)
            CALL PAPDES(SIG,NPIC,NBVOL,NVOL(I),SDEB,SFIN)
            CALL CALPHA(SIG,SMAXS,NPIC,NBVOL,NVOL(I),1,ALPHA,ALPH)
            XBID1=0.
            XBID2=0.
            CALL PREFAS(NOMPB,SIG,HMA,HMI,HO,VMICL,SMAXS,SMINSV,A,B,C,
     &                  XN,R02,RM,0,NPIC,ICOMP,1,XBID1,XBID2,NCOM)
            IF(OMEGA.NE.0) CALL EDIFIL(NFILT,1,NVOL(I))
            IF(IPAP.EQ.1) CALL PAPNIV(NOMPB,TITRE,SIG,SMAXS,NPIC,NBVOL,
     &      NVOL(I),1,PMILLE,IBID,IBID,IRMIN,IRMAX,IBID,IBID,IBID)
          ENDIF
        ELSE
          WRITE(3,'(1X,A)') '================='
          WRITE(3,'(1X,A12,I5)') 'VOL NUMERO =',NVOL(I)
          WRITE(3,'(1X,A)') '================='
          WRITE(3,'(A)') 'VOL VIDE'
          WRITE(3,'(A)')
        ENDIF
      ENDDO
C
      WRITE(3,'(/,3(A,/))')
     & ' ATTENTION: L''efficacité PREFFAS éditée dans le vol',
     & ' ---------  ne tient pas compte de l''effet de sequence',
     & '            --> Pour la vrai valeur Regarder dans le .log!' 
C
      CALL ECRSEQ(NOMPB,SMINS,SMAXS,ALPHAS,PMILLE,OMEGA,NBVOL,NTOT,
     &            NTOTF,IMINS,IMAXS,ISEVS,ISEVP,ICAL,IPAP,NIVMIN,NIVMAX,
     &            IRMIN,IRMAX,NECMIS,NVOLMI,NVOLMA)
      CALL ECRRES(NOMPB,ENDTOT,VALEUR,SMOYRE,NRE,SALTRE,RIQF,SMINS,EF,
     &            EFCL,A,B,C,XN,R02,RM,NPTREX,NPTREY,ILOI,19,NTOT,NBVOL,
     &            ICAL,IPAP,ICOMP)
      CALL FIN(4,1,0,4,0,0,1,0,6,1,1,1,1,1)
      CALL CLOSE(NOMPB,IPAP,ISTOC)
      END
C -------------------------------------------
      SUBROUTINE ANALYS(SIG,NPIC,IECRI,NUVOL,
     &                  SMINS,SMAXS,SMAXV,SMINV,IMINS,IMAXS,IMINV)
C ----------------------------------------------------------------
C ANALYSE DU VOL
C ENTREES : SIG         CONTRAINTES
C           NPIC        NOMBRE DE CONTRAINTES
C           IECRI       FLAG D'ECRITURE
C           NUVOL       NUMERO DU VOL
C SORTIES : SMINS,SMAXS CONTRAINTES MIN ET MAX SUR LA SEQUENCE
C           SMAXV       CONTRAINTE MAXIMALE DU VOL
C           SMINV       CONTRAINTE MINIMALE DU VOL
C           IMINS,IMAXS VOLS CONTENANT LES CONTRAINTES MIN ET MAX
C           IMINV       RANG DE LA CONTRAINTE MINIMALE DANS LE VOL
C ****************************************************************
      REAL SIG(*)
C
      WRITE(6,*) 'SOUS PROGRAMME ANALYS'
C
C CALCUL DU MIN ET DU MAX SUR LE VOL
C ----------------------------------
      CALL MINMAX(SIG,NPIC,SMAXV,SMINV,IMAXV,IMINV)
C
C CALCUL DU MIN ET DU MAX SUR LA SEQUENCE
C ---------------------------------------
      IF(SMINV.LE.SMINS) THEN
        SMINS = SMINV
        IMINS = NUVOL
      ENDIF  
      IF(SMAXV.GE.SMAXS) THEN
        SMAXS = SMAXV
        IMAXS = NUVOL
      ENDIF  
C
C CALCUL DE SMAX-SMIN
C -------------------
      DSMAX = SMAXV-SMINV
C
C CALCUL DE (SMAX+SMIN)/2
C -----------------------
      SMOYX = .5*(SMINV+SMAXV)
C
C CALCUL DE R
C -----------
      IF(ABS(SMAXV).LE.1E-6) THEN
        WRITE(6,*) 'SMAX = 0. R=-99. VOL NUMERO= ',NUVOL
      ELSE
        R = SMINV/SMAXV
      ENDIF
C
C CALCUL DE LA MOYENNE
C --------------------
      S = 0.
      DO I=1,NPIC
        S = S+SIG(I)
      ENDDO
      SMOY = S/FLOAT(NPIC)
C
C CALCUL DU FACTEUR D'IRREGULARITE
C --------------------------------
      N0 = 0
      DO I=1,NPIC-1
        PENTE = (SIG(I+1)-SMOY)*(SMOY-SIG(I))
        IF(PENTE.GE.0.) N0 = N0+1
      ENDDO
      FACIRR = FLOAT(N0)/FLOAT(NPIC)
C
C ECRITURE
C --------
   10 FORMAT(1X,':--------------------------------:----------------:----
     &----:-------:')
   20 FORMAT(1X,': ',A30,' : ',F14.5,' : ',A6,' : ',A5,' :') 
   30 FORMAT(1X,': ',A30,' : ',F14.5,' : ',A6,' : ',I5,' :') 
   40 FORMAT(1X,': ',A30,' : ',I14,' : ',A6,' : ',A5,' :') 
   50 FORMAT(1X,A36)
      IF(IECRI.EQ.1) THEN
        WRITE(3,'(A)') 
        WRITE(3,'(A)') 
        WRITE(3,'(1X,A)') '================='
        WRITE(3,'(1X,A12,I5)') 'VOL NUMERO =',NUVOL
        WRITE(3,'(1X,A)') '================='
C
        WRITE(3,10)
        WRITE(3,40) 'NOMBRE DE CONTRAINTES',NPIC,' ',' '
        WRITE(3,20) 'MOYENNE',SMOY,' ',' '
        WRITE(3,30) 'SMIN',SMINV,'RANG',IMINV
        WRITE(3,30) 'SMAX',SMAXV,'RANG',IMAXV
        WRITE(3,20) 'SMAX-SMIN',DSMAX,' ',' '
        WRITE(3,20) '(SMIN+SMAX)/2',SMOYX,' ',' '
        WRITE(3,20) 'RAPPORT R',R,' ',' '
        WRITE(3,20) 'FACTEUR D''IRREGULARITE',FACIRR,' ',' '
      ENDIF
C
      RETURN
      END
C --------------------------------------------------------------------
      SUBROUTINE CALPHA(SIG,SMAXS,NPIC,NBVOL,NUVOL,IECRI,ALPHA,ALPHAS)
C --------------------------------------------------------------------
C CALCUL DE alpha
C ENTREES : SIG   CONTRAINTES
C           SMAXS CONTRAINTE MAXIMALE SUR LA SEQUENCE
C           NPIC  NOMBRE DE CONTRAINTES
C           NBVOL NOMBRE DE VOLS
C           NUVOL NUMERO DU VOL
C           IECRI FLAG D'ECRITURE
C SORTIES : ALPHA ALPHA VOL
C           ALPHA ALPHA SEQUENCE
C ******************************
      REAL SIG(*)
C
C      WRITE(6,*) 'SOUS PROGRAMME CALPHA'
C
   10 FORMAT(1X,':--------------------------------:----------------:----
     &----:-------:')
   20 FORMAT(1X,': ',A30,' : ',F14.5,' : ',A6,' : ',A5,' :') 
      DATA SMAXT,NT/0.,0/
C
      CALL MINMAX(SIG,NPIC,SMAXV,SMINV,IMAXV,IMINV)
      SMAX = 0.
      N    = 0
C
      DO I=1,NPIC,2
        IF(SIG(I).GE.0.) THEN
          N    = N+1
          SMAX = SMAX+SIG(I)
        ENDIF
      ENDDO
C
      IF(N.GT.0) THEN
        ALPHA = 1.-SMAX/(N*SMAXV)
        IF(IECRI.EQ.1) WRITE(3,20) 'ALPHA',ALPHA,' ',' '
      ELSE
  100   FORMAT(' TOUTES LES CONTRAINTES SONT NEGATIVES - VOL =',I6)
        WRITE(6,100) NUVOL
        IF(IECRI.EQ.1) THEN
          WRITE(3,100) NUVOL
          ALPHA = -9999.
        ENDIF  
      ENDIF
      SMAXT = SMAXT+SMAX
      NT    = NT+N
      IF((NUVOL.EQ.NBVOL).AND.(NT.GT.0.)) THEN
        ALPHAS = 1.-SMAXT/(NT*SMAXS)
      ELSE
        ALPHAS = -9999.
      ENDIF
C
      RETURN
      END
C -----------------------------------------------------------------
      SUBROUTINE CALSIG(NOMPB,CEQUI,DELSI,IFREQ,ISEQ,IEXIST,NBPALR,
     &                  NVOL,NBSEQ,NBVOL)
C ---------------------------------------
C CALCUL DES PICS-VALLEES A PARTIR DE LA DEFINITION DES PALIERS
C ENTREES : NOMPB   NOM DU PROBLEME 
C           CEQUI   CONTRAINTES D'EQUILIBRE
C           DELSI   DELTA SIGMA
C           IFREQ   FREQUENCES
C           ISEQ    SEQUENCE (NOMBRE/NUMERO DU VOL)
C           IEXIST  =1 VOL EXISTANT
C           NBPALR  NOMBRE DE PALIERS REELS
C           NVOL    NUMERO DES VOLS EDITES
C           NBSEQ   LONGUEUR DE LA SEQUENCE
C           NBVOL   NOMBRE DE VOLS
C ********************************
      PARAMETER(MAXSIG=800000,NBPAL=20,NBVOLM=100)
C
      CHARACTER*80 NOMPB
      CHARACTER*80  MESSAG
      REAL CEQUI(NBPAL,*),DELSI(NBPAL,*),SIG(MAXSIG)
      INTEGER IFREQ(NBPAL,*),ISEQ(NBVOLM,*),IEXIST(*),NBPALR(*),NVOL(*),
     &        MVOL(5)
C   
      WRITE(6,*) 'SOUS PROGRAMME CALSIG'
C
      DO I=1,5
        MVOL(I) = 0
      ENDDO
C
      REWIND(2)
      IVOL = 0
      DO N=1,NBSEQ    
        K = 0
        L = ISEQ(N,2)
        IF(IEXIST(L).EQ.1) THEN
          DO I=1,NBPALR(L)
            DO J=1,IFREQ(I,L)
              K      = K+1
              SIG(K) = CEQUI(I,L)+DELSI(I,L)
              K      = K+1
              SIG(K) = CEQUI(I,L)-DELSI(I,L)
            ENDDO
          ENDDO
          NPIC = K
        ELSE
          NPIC = 0
        ENDIF
C
        NTOT = NTOT+NPIC
        IF(NPIC.GT.MAXSIG) THEN
  100     FORMAT(1X,'ERREUR - NPIC > 100000')
          WRITE(MESSAG,100)
          CALL ERROR(NOMPB,MESSAG,19)
        ENDIF
C
C ECRITURE
C --------
        DO L1=1,ISEQ(N,1)
          IVOL = IVOL+1
C
          DO J=1,5
            IF((MVOL(J).EQ.0).AND.(NVOL(J).EQ.L)) MVOL(J) = IVOL
          ENDDO
C
  110     FORMAT('VOL NO ',I4)
  120     FORMAT('NOMBRE DE VALEURS')
  130     FORMAT('SUITE DE VALEURS')
          WRITE(2,110) IVOL
          WRITE(2,120) 
          WRITE(2,*)   NPIC
          WRITE(2,130) 
          K     = -10
          NREST = NPIC-NPIC/10*10
          DO I=1,NPIC/10
            K = K+10
            WRITE(2,'(10(F10.5,1X))') (SIG(J+K),J=1,10)
          ENDDO
          IF(NREST.GE.1) WRITE(2,'(10(F10.5,1X))') 
     &      (SIG(NPIC/10*10+L),L=1,NPIC-NPIC/10*10)
        ENDDO  
      ENDDO  
C
      NBVOL = IVOL
      DO I=1,5
        NVOL(I) = MVOL(I)
      ENDDO
C
      RETURN
      END
C --------------------------------------
      SUBROUTINE CLOSE(NOMPB,IPAP,ISTOC)
C --------------------------------------
C DESTRUCTION DES FICHIERS
C ENTREES : NOMPB  NOM DU PROBLEME
C           NVOL   NUMERO DES VOLS EDITES
C           IPAP   =1 TRANSFORMATION EN NIVEAUX =2 NON
C           ISTOC  =1 PAS DE STOCKAGE DU FICHIER FILTRE =2 STOCKAGE
C *****************************************************************
      CHARACTER*80 NOMPB

	WRITE(6,*) 'SOUS PROGRAMME CLOSE'
C     
      IF(IPAP.EQ.2) CLOSE(7,STATUS='DELETE')
      IF(ISTOC.EQ.1) CLOSE(10,STATUS='DELETE')
      CLOSE(16)
C
      RETURN
      END
C -------------------------------------------------------
      SUBROUTINE CYCLE(NOMPB,SIG,OMEGA,SIGMAD,NPIC,NUVOL)
C -------------------------------------------------------
C FILTRAGE
C ENTREES : NOMPB  NOM DU PROBLEME
C           SIG    CONTRAINTES
C           OMEGA  VALEUR DE FILTRAGE 
C           SIGMAD LIMITE D'ENDURANCE
C           NPIC   NOMBRE DE CONTRAINTES
C           NUVOL   NUMERO DU VOL
C SORTIES : SIG    CONTRAINTES 
C           NPIC   NOMBRE DE CONTRAINTES
C***************************************
      PARAMETER(MAXSIG=800000)
C
      CHARACTER*80 NOMPB
      REAL SIG(*)
C
      WRITE(6,*) 'SOUS PROGRAMME CYCLE'
      EPS = 1E-6
C
CModif D. Grimald : le 09/01/20001 (Reactivation de ce bout de code!)
C RECHERCHE DE LA PREMIERE ALTERNANCE SIGNIFICATIVE
C -------------------------------------------------
      L     = 0
      DSMIN = 0.
      DO J=2,NPIC
        S2 = SIG(J)
        DO I=1,J
          S1 = SIG(I)
          SEUIL = FSEUIL(S1,S2,OMEGA,SIGMAD)
          IF((ABS(S1-S2).GE.SEUIL).AND.(ABS(S1-S2).GE.DSMIN)) THEN
            L     = I
            DSMIN = ABS(S1-S2)
          ENDIF
        ENDDO
        IF(L.NE.0) GOTO 1 
      ENDDO
      WRITE(6,*) 'VOL NUMERO =',NUVOL,' VIDE'
      NPIC = 0
      GOTO 3
C
    1 CONTINUE
C
C MISE A S1 DES IFIN-1 PREMIERES CONTRAINTES 
C ------------------------------------------
      DO I=1,J-1
        SIG(I) = SIG(L)
      ENDDO
      I = L
C
cccc  I = 0
CModif D. Grimald : le 09/01/20001 ----------------------------------
      DO WHILE(I.LE.NPIC-2)
        DO J=I+1,NPIC-1
          SEUIL = FSEUIL(SIG(J),SIG(J+1),OMEGA,SIGMAD)    
C
C CYCLE CONSERVE
C --------------
          IF(ABS(SIG(J)-SIG(J+1)).GE.SEUIL) GOTO 2
C 
C CYCLE ELIMINE
C -------------
          DS    = SIG(J)-SIG(I)
          IF(ABS(DS).LE.EPS) THEN
            SIGNE = 0.
          ELSE
            SIGNE = ABS(DS)/DS
          ENDIF
          SIGMA    = SIG(I)+SIGNE*AMAX1(ABS(DS),ABS(SIG(I)-SIG(J+1)))
          SIG(J)   = SIGMA
          SIG(J+1) = SIGMA
        ENDDO
    2   CONTINUE
        I = J
      ENDDO
C
    3 CONTINUE
      RETURN
      END
C ---------------------------------------------------------------------
      SUBROUTINE ECRDST(NOMPB,TITRE,AMAT,SMOYRE,NRE,SALTRE,VALEUR,RIQF,
     &                  OMEGA,CE,A,B,C,XN,R02,RM,SIGMAD,NPTREX,NPTREY,
     &                  NBVOL,ILOI,KSNUL)
C ---------------------------------------
C CONSTITUTION DU FICHIER prefixe.dossier DONNEES STRATEGIE
C ENTREES : NOMPB              NOM DU PROBLEME
C           TITRE              TITRE DU PROBLEME
C           AMAT               DESIGNATION MATERIAU
C           SMOYRE,NRE,SALTRE  RESEAU Smoy,N,Salt
C           VALEUR             VALEURS MATERIAUX
C           RIQF               IQF
C           OMEGA              VALEUR DE FILTRAGE 
C           CE,A,B,C,XN,R02,RM COEFFICIENTS ELBER
C           SIGMAD             LIMITE D'ENDURANCE
C           NPTREX             NOMBRE DE POINTS RESEAU Smoy
C           NPTREY             NOMBRE DE POINTS RESEAU N
C           NBVOL              NOMBRE DE VOLS
C           ILOI               LOI UTILISEE
C           KSNUL              PARAMETRE D'ELIMINATION DES SPECTRES
C *****************************************************************
      PARAMETER(MAXCAL=60,NRESX=5)
C
      CHARACTER*80 NOMPB

	CHARACTER*(*) TITRE,AMAT
      REAL SMOYRE(MAXCAL,*),NRE(MAXCAL,*),SALTRE(MAXCAL,NRESX,*),
     &     VALEUR(MAXCAL,*)
      INTEGER NPTREX(*),NPTREY(*)
C
      WRITE(6,*) 'SOUS PROGRAMME ECRDST'
C
   70 FORMAT(1X,A26,1X,E14.6)
   80 FORMAT(1X,A26,1X,A)
  290 FORMAT(' PROBLEME : ',A)
  300 FORMAT(' TITRE    : ',A)
  310 FORMAT(1X,':   Smoy(MPa)   :    N(vols)    :   Salt(MPa)   :')
  315 FORMAT(1X,':---------------:---------------:---------------:')
  320 FORMAT(1X,': ',E13.6,' : ',E13.6,' : ',E13.6,' :')
  325 FORMAT(1X,': ',A13,' : ',E13.6,' : ',E13.6,' :')
  440 FORMAT(1X,':--------------------:---------------:')
  450 FORMAT(1X,': ',A18,' : ',E13.6,' :')
C
      CALL STOP(NOMPB,'enchainement',IFLAG)
      IF(IFLAG.EQ.0) THEN
        CALL ENTETE(3,'EXECUTION CHARGE')
C
        WRITE(3,'(A)') 
        WRITE(3,290) NOMPB(IFDEB(NOMPB):IFFIN(NOMPB))
        WRITE(3,300) TITRE(IFDEB(TITRE):IFFIN(TITRE))
C
C LECTURE DES DONNEES D'ENTREE
C ----------------------------
        CALL LECABRE(NOMPB,2,0,2,19,3,8)
      ENDIF
C
      WRITE(3,'(A)') 
      WRITE(3,'(A)') 
      WRITE(3,'(A)') ' ================='
      WRITE(3,'(A)') ' DONNEES STRATEGIE'
      WRITE(3,'(A)') ' ================='
      WRITE(3,80) 'MATERIAU DB              =',
     &             AMAT(IFDEB(AMAT):IFFIN(AMAT))
      WRITE(3,'(1X,A)') 'COEFFICIENTS MATERIAUX'
C
C AMORCAGE
C --------
      WRITE(3,'(1X,A)') 'AMORCAGE'
      IF(ILOI.EQ.1) THEN
        WRITE(3,'(1X,A)') 'RESEAU'
        WRITE(3,315) 
        WRITE(3,310) 
        WRITE(3,315) 
        DO J=1,NPTREX(1)
          DO K=1,NPTREY(1)
            IF(K.EQ.1) THEN
              WRITE(3,320) SMOYRE(1,J),NRE(1,K),SALTRE(1,J,K)
            ELSE
              WRITE(3,325) ' ',NRE(1,K),SALTRE(1,J,K)
            ENDIF
          ENDDO
          WRITE(3,315) 
        ENDDO
      ELSEIF(ILOI.EQ.2) THEN
        WRITE(3,440)
        WRITE(3,450) 'PENTE p',VALEUR(1,1)
        WRITE(3,450) 'PENTE q',VALEUR(1,2)
        WRITE(3,440)
      ELSEIF(ILOI.EQ.3) THEN
        WRITE(3,440)
        WRITE(3,450) 'PENTE p',VALEUR(1,1)
        WRITE(3,440)
      ENDIF
C
C DONNEES PROPA
C -------------
      WRITE(3,'(1X,A)') 'ELBER'
      WRITE(3,440)
      WRITE(3,450) 'Ceff',CE
      WRITE(3,450) 'A',A
      WRITE(3,450) 'B',B
      WRITE(3,450) 'C',C
      WRITE(3,450) 'XN',XN
      WRITE(3,450) 'R02 (MPa)',R02
      WRITE(3,450) 'RM (MPa)',RM
      WRITE(3,440)
C
C DONNEES FILTRAGE
C ----------------
      IF(OMEGA.NE.0.) THEN
        WRITE(3,'(1X,A)') 'FILTRAGE'
        WRITE(3,440)
        WRITE(3,450) 'Sd',SIGMAD
        WRITE(3,440)
      ENDIF
C
      RETURN
      END
C -------------------------------
      SUBROUTINE ECRIDE(NOMPB,IU)
C -------------------------------
C ECRITURE DES DONNEES STRATEGIE ET POSITIONNEMENT SUR LE FICHIER SIGMA
C ENTREES : NOMPB   NOM DU PROBLEME
C           IU      UNITE DU FICHIER SIGMA
C ****************************************
      CHARACTER*80 NOMPB
      CHARACTER*120 CARTE
      CHARACTER*80  MESSAG
C
      WRITE(6,*) 'SOUS PROGRAMME STRATE'
      ILIG = 0
C
      REWIND(2)
      REWIND(IU)
C
C DEBUT DU FICHIER
C ----------------
    1 READ(2,'(A)',END=2,ERR=900) CARTE
      ILIG = ILIG+1
      IF(INDEX(CARTE,'ABRE').NE.0)
     &  WRITE(IU,'(A)') CARTE(IFDEB(CARTE):IFFIN(CARTE))
      GOTO 1
    2 CONTINUE
      RETURN
C
  901 FORMAT(1X,'ERREUR A LA LECTURE DU FICHIER SIGMA LIGNE=',I3)
  900 WRITE(MESSAG,901) ILIG
      CALL ERROR(NOMPB,MESSAG,19)
      END
C -------------------------------------------------------------
      SUBROUTINE ECRFIL(NOMPB,SIG,NFILT,IECRI,NUVOL,NPIC,NTOTF)
C -------------------------------------------------------------
C ECRITURE DU FICHIER SIGMA FILTRE
C ENTREES : NOMPB   NOM DU PROBLEME
C           SIG     CONTRAINTES
C           NFILT   NOMBRE DE CONTRAINTES FILTREES PR VOL
C           IECRI   FLAG D'ECRITURE
C           NUVOL   NUMERO DU VOL
C           NPIC    NOMBRE DE CONTRAINTES
C SORTIES : NTOTF   NOMBRE DE CONTRAINTES FILTREES SUR LA SEQUENCE
C ****************************************************************
      CHARACTER*80 NOMPB
      REAL SIG(*)
      INTEGER NFILT(*)
C  
      WRITE(6,*) 'SOUS PROGRAMME ECRFIL'
C
   10 FORMAT(1X,':--------------------------------:----------------:----
     &----:-------:')
   20 FORMAT(1X,': ',A30,' : ',I14,' : ',A6,' : ',A5,' :') 
  110 FORMAT('VOL NO ',I4)
  120 FORMAT('NOMBRE DE VALEURS')
  130 FORMAT('SUITE DE VALEURS')
      WRITE(10,110) NUVOL
      WRITE(10,120) 
      WRITE(10,*) NPIC
      WRITE(10,130) 
      L2   = -10
      NREST = NPIC-NPIC/10*10
      DO L3=1,NPIC/10
        L2 = L2+10
        WRITE(10,'(10(F10.5,1X))') (SIG(N+L2),N=1,10)
      ENDDO
      IF(NREST.GE.1) WRITE(10,'(10(F10.5,1X))') 
     &               (SIG(NPIC/10*10+L3),L3=1,NREST)
C  
      NTOTF = NTOTF+NFILT(NUVOL)
C
      IF(IECRI.EQ.1) THEN
        WRITE(3,'(A)') 
        WRITE(3,'(A)') 
        WRITE(3,'(1X,A)') '================='
        WRITE(3,'(1X,A12,I5)') 'VOL NUMERO =',NUVOL
        WRITE(3,'(1X,A)') '================='
        WRITE(3,10)
        WRITE(3,20) 'NOMBRE DE CONTRAINTES FILTREES',NFILT(NUVOL),' ',
     &              ' '
        WRITE(3,10)
      ENDIF
C
      RETURN
      END
C ----------------------------------------------------------------
      SUBROUTINE ECRPAL(NOMPB,DESIG,CEQUI,DELSI,ISEQ,IFREQ,IEXIST,
     &                  NBPALR,NBSEQ)
C -----------------------------------
C ECRITURE DES PALIERS
C ENTREES : NOMPB   MOM DU PROBLEME
C           DESIG   DESIGNATION DES PALIERS
C           CEQUI   CONTRAINTES D'EQUILIBRE
C           DELSI   DELTA SIGMA
C           IFREQ   FREQUENCES
C           ISEQ    SEQUENCE (NOMBRE,NUMERO DU VOL)
C           IEXIST  =1 VOL EXISTANT
C           NBPALR  NOMBRE DE PALIERS REELS
C           NBSEQ   LONGUEUR DE LA SEQUENCE
C *****************************************
      PARAMETER(NBPAL=20,NBVOLM=100)
C
      CHARACTER*80 NOMPB

	CHARACTER*(*) DESIG(NBPAL,*)
      REAL CEQUI(NBPAL,*),DELSI(NBPAL,*)
      INTEGER IFREQ(NBPAL,*),ISEQ(NBVOLM,*),NBPALR(*),IEXIST(*)
C
      WRITE(6,*) 'SOUS PROGRAMME ECRPAL'
C
   10 FORMAT(1X,':----------------------:----:')
   20 FORMAT(1X,': ',A20,' : ',I2' :') 
  310 FORMAT(1X,':  Designation  :   Sequi(MPa)  :  Dsigma(MPa)  :  Freq
     &uence    :')
  315 FORMAT(1X,':---------------:---------------:---------------:------
     &---------:')
  325 FORMAT(1X,': ',A13,' : ',E13.6,' : ',E13.6,' : ',4X,I5,4X,' : ')
C
   40 FORMAT(1X,':--------:--------:')
   50 FORMAT(1X,': ',I6,' : ',I6' :') 
   60 FORMAT(1X,': Nombre : Numero :')
C
      WRITE(3,'(A)')
      WRITE(3,'(A)')
      WRITE(3,'(A)') ' DEFINITION DES PALIERS'
      DO I=1,NBVOLM
        IF(IEXIST(I).NE.0) THEN
          WRITE(3,10)
          WRITE(3,20) 'VOL NUMERO',I
          WRITE(3,10)
C
          WRITE(3,315)
          WRITE(3,310)
          WRITE(3,315)
          DO J=1,NBPALR(I)
            WRITE(3,325) DESIG(J,I),CEQUI(J,I),DELSI(J,I),IFREQ(J,I)
          ENDDO
          WRITE(3,315)
        ENDIF  
      ENDDO   
C
      WRITE(3,'(A)')
      WRITE(3,'(A)')
      WRITE(3,'(A)') ' DEFINITION DE LA SEQUENCE'
      WRITE(3,40)
      WRITE(3,60)
      WRITE(3,40)
      DO I=1,NBSEQ
        WRITE(3,50) ISEQ(I,1),ISEQ(I,2)
      ENDDO
      WRITE(3,40)
C
      RETURN
      END
C -----------------------------------------------------------------
      SUBROUTINE ECRRES(NOMPB,ENDTOT,VALEUR,SMOYRE,NRE,SALTRE,RIQF,
     &                  SMINS,EF,EFCL,A,B,C,XN,R02,RM,NPTREX,NPTREY,
     &                  ILOI,IU,NTOT,NBVOL,ICAL,IPAP,ICOMP)
C ---------------------------------------------------------
C CONSTITUTION DU FICHIER prefixe.dossier DUREE DE VIE
C ENTREES : NOMPB             NOM DU PROBLEME
C           ENDTOT            ENDOMMAGEMENT TOTAL
C           VALEUR            COEFFICIENTS MATERIAUX
C           SMOYRE,NRE,SALTRE RESEAU Smoy,N,Salt
C           RIQF              IQF
C           SMINS             CONTRAINTE MIN SEQUENCE
C           EF                EFFICACITE
C           EFCL              EFFICACITE EN CUMUL LINEAIRE
C           A,B,C,XN,R02,RM   COEFFICIENTS ELBER
C           NPTREX            NOMBRE DE POINTS RESEAU Smoy
C           NPTREY            NOMBRE DE POINTS RESEAU N
C           ILOI              LOI UTILISEE
C           IU                UNITE DU FICHIER prefixe.erreurs
C           NTOT              NOMBRE DE CONTRAINTES TOTALES
C           NBVOL             NOMBRE DE VOL
C           ICAL              =1 AMORCAGE =2 PROPAGATION =3 AMORCAGE+PROPAGATION
C           IPAP              =1 TRANSFORMATION EN NIVEAUX =2 NON
C           ICOMP             =1 PRISE EN COPTE DE LA COMPRESSION
C ***************************************************************
      PARAMETER(MAXCAL=60,NRESX=5,NRESY=9)
C
      CHARACTER*80 NOMPB
      CHARACTER*80  NATURE
      REAL VALEUR(MAXCAL,*),SMOYRE(MAXCAL,*),NRE(MAXCAL,*),
     &     SALTRE(MAXCAL,NRESX,*),X(NRESY),Y(NRESY),Z(NRESX),
     &     XTRA(3),YTRA(3),SMOYD(200),RD(200)
      INTEGER NPTREX(*),NPTREY(*)
      DOUBLE PRECISION SMAXEQ,DXN
C
      WRITE(6,*) 'SOUS PROGRAMME ECRRES'
C
      EPS  = 1E-6
      ISUC = 0
   10 FORMAT(1X,':--------------------------------:----------------:')
   20 FORMAT(1X,': ',A30,' : ',F14.0,' :') 
   30 FORMAT(1X,': ',A30,' : ',F14.5,' :') 
   40 FORMAT(1X,': ',A30,' : ',E14.6,' :') 
C
      IF((ICAL.EQ.1).OR.(ICAL.EQ.3)) THEN
        WRITE(3,'(A)') 
        WRITE(3,'(A)') 
        WRITE(3,'(A)') ' ====================='
        WRITE(3,'(A)') ' DUREE DE VIE AMORCAGE'
        WRITE(3,'(A)') ' ====================='
        IF(ENDTOT.LE.EPS) THEN
          WRITE(3,'(1X,A)') 'ENDOMMAGEMENT TOTAL NUL'
          WRITE(6,*) 'ENDOMMAGEMENT =',ENDTOT
        ELSE
          WRITE(3,10) 
          WRITE(3,20) 'NOMBRE DE VOLS ADMISSIBLES',FLOAT(NBVOL)/ENDTOT
          WRITE(3,10) 
          WRITE(6,*) 'ENDOMMAGEMENT SEQUENCE =',ENDTOT
          WRITE(6,*) 'NOMBRE DE VOLS ADMISS. =',FLOAT(NBVOL)/ENDTOT
C
          IF(ILOI.EQ.1) THEN
C
C CALCUL DE Seq POUR LA LOI RESEAU
C --------------------------------
            IF(FLOAT(NBVOL)/ENDTOT.LT.NRE(1,1)) THEN
              WRITE(6,'(A)') ' Nadm < NREmin'
            ELSEIF(FLOAT(NBVOL)/ENDTOT.GT.NRE(1,NPTREY(1))) THEN
              WRITE(6,'(A)') ' Nadm > NREmax'
            ELSE
              DO I=1,NPTREX(1)
                DO J=1,NPTREY(1)
                  X(J) = LOG10(NRE(1,J))
                  Y(J) = SALTRE(1,I,J)
                ENDDO
                Z(I) = FINT(NOMPB,'N',LOG10(FLOAT(NBVOL)/ENDTOT),X,Y,IU,
     &                      NPTREY(1),2,0)
                CALL ENDOR(NOMPB,SMOYRE(1,I),Z(I),SMOYRE,NRE,SALTRE,
     &                     NPTREX,NPTREY,19,ENDOM)
                WRITE(6,*) 'Salt,Nadm =',Z(I),1./ENDOM
              ENDDO
C
              WRITE(3,'(A)')
              WRITE(3,'(1X,A)') 'CYCLE EQUIVALENT AMORCAGE A R=0.1'
              WRITE(3,10) 
              L = 0
              DO K=1,200
                SMOYD(K) = (FLOAT(K)-1.)*(SMOYRE(1,NPTREX(1))-
     &                     SMOYRE(1,1))/199.+SMOYRE(1,1)
                DO I=1,NPTREX(1)
                  X(I) = SMOYRE(1,I)
                ENDDO
                SALTD = FINT(NOMPB,'Smoy',SMOYD(K),X,Z,IU,NPTREX(1),3,0)
                RD(K) = (SMOYD(K)-SALTD)/(SMOYD(K)+SALTD)
              ENDDO
C               
              DO I=1,199
                DO J=I+1,200
                  IF(RD(J).LT.RD(I)) THEN
                    TAMP1    = RD(J)
                    TAMP2    = SMOYD(J)
                    RD(J)    = RD(I)
                    SMOYD(J) = SMOYD(I)
                    RD(I)    = TAMP1
                    SMOYD(I) = TAMP2
                  ENDIF 
                ENDDO 
              ENDDO  
C               
              SMOY   = FINT(NOMPB,'R',.1,RD,SMOYD,IU,200,2,0)
              SALT   = SMOY*(1.-.1)/(1.+.1)
              SMAXEQ = SMOY+SALT
              SMINEQ = 0.1*SMAXEQ
              R      = (SMOY-SALT)/(SMOY+SALT)
              WRITE(6,*) 'SMOY,SALT TROUVE =',SMOY,SALT,R
              CALL ENDOR(NOMPB,SMOY,SALT,SMOYRE,NRE,SALTRE,NPTREX,
     &                   NPTREY,19,ENDOM)
              WRITE(6,*) 'Nadm             =',1./ENDOM
              WRITE(3,30) 'SMIN equivalent (MPa)',SMINEQ
              WRITE(3,30) 'SMAX equivalent (MPa)',SMAXEQ
              WRITE(3,10) 
            ENDIF
C
C CALCUL DE Seq POUR LES LOIS MANUEL
C ----------------------------------
          ELSEIF((ILOI.EQ.2).OR.(ILOI.EQ.3)) THEN
            ISUC   = 1
            SMAXEQ = RIQF*(1E-5*FLOAT(NBVOL)/ENDTOT)**(1./VALEUR(1,1))
          ENDIF
C
          IF(ISUC.EQ.1) THEN
            SMINEQ = 0.1*SMAXEQ
            WRITE(3,'(A)')
            WRITE(3,'(1X,A)') 'CYCLE EQUIVALENT AMORCAGE A R=0.1'
            WRITE(3,10) 
            WRITE(3,30) 'SMIN equivalent amor (MPa)',SMINEQ
            WRITE(3,30) 'SMAX equivalent amor (MPa)',SMAXEQ
            WRITE(3,10) 
C
C TRACE DU CYCLE EQUIVALENT EN AMORCAGE
C -------------------------------------
  396       FORMAT('! TRACE Sequi amorc=f(t)')
            WRITE(4,'(A)')
            WRITE(4,398)
            WRITE(4,396)
            WRITE(4,398)
            XTRA(1) = 1.
            YTRA(1) = SMINEQ
            XTRA(2) = INT(NTOT/(2.*NBVOL))
            YTRA(2) = SMAXEQ
            XTRA(3) = NINT(FLOAT(NTOT/NBVOL))
            YTRA(3) = SMINEQ
            NATURE = 'Sequi-amorc=f(t)'
            CALL DEBUT(NOMPB,NATURE,'temps','S (MPa)',XTRA,YTRA,3,4)
          ENDIF
        ENDIF  
      ENDIF
C
      IF((ICAL.EQ.2).OR.(ICAL.EQ.3)) THEN
C
C CALCUL DE Sequi EN PROPA
C ------------------------
        IF(EF.LE.0) THEN
          WRITE(3,'(1X,A)') 'EFFICACITE NULLE'
        ELSE
          U      = A+B*.1+C*.1**2
          DXN=XN
          SMAXEQ = (EF/(NBVOL*1.))**(1./DXN)*(1./(.9*U))
          SMINEQ = 0.1*SMAXEQ
          WRITE(3,'(A)')
          WRITE(3,'(1X,A)') 'CYCLE EQUIVALENT PROPA A R=0.1'
          WRITE(3,10) 
          WRITE(3,40) ' SEQUENCE EFFICACITE (MPa**n) ',EF
          WRITE(3,40) ' SEQ EFFICACITE LINE (MPa**n) ',EFCL
          WRITE(3,30) 'SMIN equivalent propa (MPa)',SMINEQ
          WRITE(3,30) 'SMAX equivalent propa (MPa)',SMAXEQ
          WRITE(3,10) 
          WRITE(6,*) 'EFFICACITE (MPa**n)  ',EF
          WRITE(6,*) 'EFFICACITE (Hb **n)  ',EF/10**XN
C
C TRACE DU CYCLE EQUIVALENT EN PROPAGATION
C ----------------------------------------
  398     FORMAT('! ======================')
  400     FORMAT('! TRACE Sequi propa=f(t)')
          WRITE(4,'(A)')
          WRITE(4,398)
          WRITE(4,400)
          WRITE(4,398)
          XTRA(1) = 1.
          YTRA(1) = SMINEQ
          XTRA(2) = NINT(NTOT/(2.*NBVOL))
          YTRA(2) = SMAXEQ
          XTRA(3) = NINT(FLOAT(NTOT/NBVOL))
          YTRA(3) = SMINEQ
          NATURE = 'Sequi-propa=f(a)'
          CALL DEBUT(NOMPB,NATURE,'temps','S (MPa)',XTRA,YTRA,3,4)
        ENDIF
      ENDIF
C
      RETURN
      END
C -----------------------------------------------------------------
      SUBROUTINE ECRSEQ(NOMPB,SMINS,SMAXS,ALPHAS,PMILLE,OMEGA,NBVOL,
     &                  NTOT,NTOTF,IMINS,IMAXS,ISEVS,ISEVP,ICAL,IPAP,
     &                  NIVMIN,NIVMAX,IRMIN,IRMAX,NECMIS,NVOLMI,NVOLMA)
C ---------------------------------------------------------------------
C CONSTITUTION DU FICHIER prefixe.dossier ANALYSE DE LA SEQUENCE
C ENTREES : NOMPB       NOM DU PROBLEME
C           SMINS,SMAXS CONTRAINTES MIN ET MAX DE LA SEQUENCE
C           ALPHAS      ALPHA SEQUENCE
C           PMILLE      FACTEUR DE CONVERSION NIVEAU-CONTRAINTE
C           NBVOL       NOMBRE DE VOLS
C           NTOT        NOMBRE DE CONTRAINTES TOTALES
C           NTOTF        NOMBRE DE CONTRAINTES APRES FILTRAGE
C           IMINS,IMAXS VOLS CONTENANT LE MIN ET LE MAX DE LA SEQUENCE
C           ISEVS       VOL LE PLUS SEVERE AU SENS DU RAIN-FLOW
C           ISEVP       VOL LE PLUS SEVERE AU SENS PREFFAS
C           ICAL        =1 AMORCAGE =2 PROPAGATION =3 AMORCAGE+PROPAGATION
C           IPAP        =1 TRANSFORMATION EN NIVEAUX =2 NON
C           NIVMIN      NIVEAU MIN SUR LA SEQUENCE
C           NIVMAX      NIVEAU MAX SUR LA SEQUENCE
C           IRMIN       RANG DU NIVEAU MIN
C           IRMAX       RANG DU NIVEAU MAX
C           NECMIS      PLUS PETIT ECART EN NIVEAU SUR LA SEQUENCE
C *************************************************************
      CHARACTER*80 NOMPB
C
      WRITE(6,*) 'SOUS PROGRAMME ECRSEQ'
C
   10 FORMAT(1X,':--------------------------------:----------------:----
     &----:-------:')
   20 FORMAT(1X,': ',A30,' : ',I14,' : ',A6,' : ',A5,' :') 
   30 FORMAT(1X,': ',A30,' : ',F14.5,' : ',A6,' : ',A5,' :')
   40 FORMAT(1X,': ',A30,' : ',F14.5,' : ',A6,' : ',I5,' :')
   60 FORMAT(1X,A36,1X,I10)
   70 FORMAT(1X,A36,1X,F10.5,1X,A6,I5)
   80 FORMAT(1X,A36,1X,F10.5)
  110 FORMAT(1X,':-------------------------------------------:----------
     &------:--------:---------:--------:------:')
  120 FORMAT(1X,': ',A41,' : ',I14,' : ',A6,' : ',I7,' : ',A6,' : ',I4,'
     & :') 
  130 FORMAT(1X,': ',A41,' : ',I14,' : ',A6,' : ',A7,' : ',A6,' : ',A4,'
     & :') 
  140 FORMAT(1X,': ',A41,' : ',F14.5,' : ',A6,' : ',A7,' : ',A6,' : ',A4
     &,' :') 
C
      WRITE(3,'(A)') 
      WRITE(3,'(A)') 
      WRITE(3,'(A)') ' ======================'
      WRITE(3,'(A)') ' ANALYSE DE LA SEQUENCE'
      WRITE(3,'(A)') ' ======================'
      WRITE(3,10) 
      WRITE(3,20) 'NOMBRE DE VOLS',NBVOL,' ',' '
      WRITE(3,20) 'NOMBRE DE CONTRAINTES TOTALES',NTOT,' ',' '
      IF(OMEGA.NE.0) WRITE(3,20) 'NOMBRE DE CONTRAINTES FILTREES',
     &NTOTF,' ',' '
      WRITE(3,40) 'SMIN',SMINS,'VOL',IMINS
      WRITE(3,40) 'SMAX',SMAXS,'VOL',IMAXS
      IF((ICAL.EQ.1).OR.(ICAL.EQ.3)) WRITE(3,20) 'VOL LE PLUS SEVERE - R
     &AIN-FLOW',ISEVS,' ',' '
      IF((ICAL.EQ.2).OR.(ICAL.EQ.3)) WRITE(3,20) 'VOL LE PLUS SEVERE - P
     &REFFAS',ISEVP,' ',' '
      WRITE(3,30) 'RAPPORT R',SMINS/SMAXS,' ',' '
      IF((ICAL.EQ.2).OR.(ICAL.EQ.3)) WRITE(3,30) 'alpha SEQUENCE',
     &                                           ALPHAS,' ',' ' 
      WRITE(3,10) 
C
      IF(IPAP.EQ.1) THEN
        WRITE(3,110) 
        WRITE(3,120) 'NIVEAU MAX',NIVMAX,'CYCLE',IRMAX,'VOL',NVOLMA
        WRITE(3,120) 'NIVEAU MIN',NIVMIN,'CYCLE',IRMIN,'VOL',NVOLMI
        WRITE(3,130) 'PLUS PETIT ECART PIC-VALLEE EN NIVEAU',NECMIS,' ',
     &' ',' ',' '
        WRITE(3,140) 'CONTRAINTE MAX',FLOAT(NIVMAX)/PMILLE,' ',' ',' ','
     & '
        WRITE(3,140) 'CONTRAINTE MIN',FLOAT(NIVMIN)/PMILLE,' ',' ',' ','
     & '
        WRITE(3,140) 'PLUS PETIT ECART PIC-VALLEE EN CONTRAINTE',
     &               FLOAT(NECMIS)/PMILLE,' ',' ',' ',' '
        WRITE(3,110) 
      ENDIF
C
      RETURN
      END
C -----------------------------------------------------------
      SUBROUTINE ECRSIM(NOMPB,SIGSIM,ISEQ,IEXIST,NPICS,NBSEQ)
C -----------------------------------------------------------
C ECRITURE DES SIGMA
C ENTREES : NOMPB   NOM DU PROBLEME
C           SIGSIM  CONTRAINTES
C           ISEQ    SEQUENCE
C           IEXIST  =1 VOL EXISTANT
C           NPICS   NOMBRE DE PICS PAR VOL
C           NBSEQ   LONGUEUR DE LA SEQUENCE
C *****************************************
      PARAMETER(MAXSIG=800000,NBVOLM=100)
C
      CHARACTER*80 NOMPB
      CHARACTER*1   BLANC(5)
      REAL SIGSIM(MAXSIG/1000,*)
      INTEGER ISEQ(NBVOLM,*),IEXIST(*),NPICS(*)
C
      WRITE(6,*) 'SOUS PROGRAMME ECRSIM'
C
   10 FORMAT(1X,':----------------------:----:')
   20 FORMAT(1X,': ',A20,' : ',I2' :') 
  310 FORMAT(1X,':Suite S(i)(MPa):               :               :       
     &         :               :')
  315 FORMAT(1X,':---------------:---------------:---------------:------
     &---------:---------------:')
  325 FORMAT(1X,': ',E13.6,' : ',E13.6,' : ',E13.6,' : ',E13.6,' : ',E13
     &.6,' :')
  335 FORMAT(1X,': ',E13.6,' : ',E13.6,' : ',E13.6,' : ',E13.6,' : ',A13
     &,' :')
  345 FORMAT(1X,': ',E13.6,' : ',E13.6,' : ',E13.6,' : ',A13,' : ',A13,'
     & :')
  355 FORMAT(1X,': ',E13.6,' : ',E13.6,' : ',A13,' : ',A13,' : ',A13,' :
     &')
  365 FORMAT(1X,': ',E13.6,' : ',A13,' : ',A13,' : ',A13,' : ',A13,' :')
C
   40 FORMAT(1X,':--------:--------:')
   50 FORMAT(1X,': ',I6,' : ',I6' :') 
   60 FORMAT(1X,': Nombre : Numero :')
C
      WRITE(3,'(A)')
      WRITE(3,'(A)')
      WRITE(3,'(A)') ' DEFINITION DU SPECTRE SIMPLIFIE'
      DO I=1,NBVOLM
        IF(IEXIST(I).NE.0) THEN
          WRITE(3,10)
          WRITE(3,20) 'VOL NUMERO',I
          WRITE(3,10)
C
          WRITE(3,315)
          WRITE(3,310)
          WRITE(3,315)
C
          DO K=1,5
            BLANC(K) = ' '
          ENDDO
          K = -5
C
          DO L=1,NPICS(I)/5
            K = K+5
            WRITE(3,325) (SIGSIM(J+K,I),J=1,5)
          ENDDO
C
          NREST = NPICS(I)-NPICS(I)/5*5
          IF(NREST.EQ.1) WRITE(3,365) 
     &    (SIGSIM(NPICS(I)/5*5+L,I),L=1,NREST),(BLANC(L),L=NREST+1,5)
          IF(NREST.EQ.2) WRITE(3,355) 
     &    (SIGSIM(NPICS(I)/5*5+L,I),L=1,NREST),(BLANC(L),L=NREST+1,5)
          IF(NREST.EQ.3) WRITE(3,345) 
     &    (SIGSIM(NPICS(I)/5*5+L,I),L=1,NREST),(BLANC(L),L=NREST+1,5)
          IF(NREST.EQ.4) WRITE(3,335) 
     &    (SIGSIM(NPICS(I)/5*5+L,I),L=1,NREST),(BLANC(L),L=NREST+1,5)
          WRITE(3,315)
        ENDIF
      ENDDO
C
      WRITE(3,'(A)')
      WRITE(3,'(A)')
      WRITE(3,'(A)') ' DEFINITION DE LA SEQUENCE'
      WRITE(3,40)
      WRITE(3,60)
      WRITE(3,40)
      DO I=1,NBSEQ
        WRITE(3,50) ISEQ(I,1),ISEQ(I,2)
      ENDDO
      WRITE(3,40)
C
      RETURN
      END
C ----------------------------------------
      SUBROUTINE EDIFIL(NFILT,IECRI,NUVOL)
C ----------------------------------------
C EDITION DU NOMBRE E CONTRAINTES FILTREES
C ENTREES : NFILT   NOMBRE DE CONTRAINTES FILTREES PR VOL
C           IECRI   FLAG D'ECRITURE
C           NUVOL   NUMERO DU VOL
C *******************************
      INTEGER NFILT(*)
C  
      WRITE(6,*) 'SOUS PROGRAMME EDIFIL'
C
   10 FORMAT(1X,':--------------------------------:----------------:----
     &----:-------:')
   20 FORMAT(1X,': ',A30,' : ',I14,' : ',A6,' : ',A5,' :') 
C
      IF(IECRI.EQ.1) THEN
        WRITE(3,20) 'NOMBRE DE CONTRAINTES FILTREES',NFILT(NUVOL),' ',
     &              ' '
        WRITE(3,10)
      ENDIF
C
      RETURN
      END
C -----------------------------------------------------
      SUBROUTINE EDITIO(NVOL,IMINS,IMAXS,ISEVS,ISEVP,K)
C -----------------------------------------------------
C ENTREES : NVOL   TABLEAU DES VOLS EDITABLES
C           IMINS  VOL CONTENANT LE MIN DE LA SEQUENCE
C           IMAXS  VOL CONTENANT LE MAX DE LA SEQUENCE
C           ISEVS  VOL LE PLUS SEVERE RAIN-FLOW
C           ISEVP  VOL LE PLUS SEVERE PREFFAS
C SORTIES : NVOL   TABLEAU DES VOLS EDITABLES
C           K      DIMENSION DU TABLEAU
C *************************************
      INTEGER NVOL(*),MVOL(7)
C
      WRITE(6,*) 'SOUS PROGRAMME EDITIO'
      K = 0
C
      DO I=1,5
        IF(NVOL(I).GT.0) THEN
          K       = K+1
          MVOL(K) = NVOL(K)
        ENDIF
      ENDDO
C
C VOL CONTENANT LE MIN DE LA SEQUENCE
C -----------------------------------
      CALL INIECR(NVOL,IMINS,IECRI)
      IF((IMINS.GT.0).AND.(IECRI.EQ.0)) THEN
        K       = K+1
        MVOL(K) = IMINS
      ENDIF
C
C VOL CONTENANT LE MAX DE LA SEQUENCE
C -----------------------------------
      IF(IMAXS.NE.IMINS) THEN
        CALL INIECR(NVOL,IMAXS,IECRI)
        IF((IMAXS.GT.0).AND.(IECRI.EQ.0)) THEN
          K       = K+1
          MVOL(K) = IMAXS
        ENDIF
      ENDIF  
C
C VOL LE PLUS SEVERE AU SENS RAINFLOW
C -----------------------------------
      IF((ISEVS.NE.IMINS).AND.(ISEVS.NE.IMAXS)) THEN
        CALL INIECR(NVOL,ISEVS,IECRI)
        IF((ISEVS.GT.0).AND.(IECRI.EQ.0)) THEN
          K       = K+1
          MVOL(K) = ISEVS
        ENDIF
      ENDIF  
C
C VOL LE PLUS SEVERE AU SENS PREFFAS
C ----------------------------------
      IF((ISEVP.NE.IMINS).AND.(ISEVP.NE.IMAXS).AND.(ISEVP.NE.ISEVS))
     &  THEN
        CALL INIECR(NVOL,ISEVP,IECRI)
        IF((ISEVP.GT.0).AND.(IECRI.EQ.0)) THEN
          K       = K+1
          MVOL(K) = ISEVP
        ENDIF
      ENDIF
C
C SAUVEGARDE
C ----------
      DO I=1,K
        NVOL(I) = MVOL(I)
      ENDDO
C
      RETURN
      END
C --------------------------------------------------
      SUBROUTINE ENDOL2(SMOY,SALT,VALEUR,RIQF,ENDOM)
C --------------------------------------------------
C CALCUL DE L'ENDOMMAGEMENT D'UN CYCLE (SMOY,SALT) PAR LA NOUVELLE 
C LOI MANUEL
C ENTREES : SMOY        CONTRAINTE MOYENNE DU CYCLE
C           SALT        CONTRAINTE ALTERNEE DU CYCLE
C           VALEUR      COEFFICIENTS MATERIAUX
C           RIQF        IQF           
C SORTIES : ENDOM       ENDOMMAGEMENT
C ***********************************
      PARAMETER(MAXCAL=60)
C
      REAL VALEUR(MAXCAL,*) 
C
      EPS   = 1E-6
      SIG01 = (2.*SALT/.9)**VALEUR(1,2)
      SIG01 = SIG01*(SMOY+SALT)**(1.-VALEUR(1,2))
C
      IF(ABS(SIG01).LE.EPS) THEN
        ENDOM = 0.
      ELSE
        ENDOM = 1E-5*(SIG01/RIQF)**(-VALEUR(1,1))
      ENDIF
C
      RETURN
      END
C ---------------------------------------------
      SUBROUTINE ENDOL3(SMOY,SALT,VALEUR,ENDOM)
C ---------------------------------------------
C CALCUL DE L'ENDOMMAGEMENT D'UN CYCLE (SMOY,SALT) PAR LA LOI
C DU MIL-HANDBOOK
C ENTREES : SMOY        CONTRAINTE MOYENNE DU CYCLE
C           SALT        CONTRAINTE ALTERNEE DU CYCLE
C           VALEUR      COEFFICIENTS MATERIAUX
C           RIQF        IQF           
C SORTIES : ENDOM       ENDOMMAGEMENT
C ***********************************
      PARAMETER(MAXCAL=60)
C
      REAL VALEUR(MAXCAL,*) 
C
      R    = (SMOY-SALT)/(SMOY+SALT)
      SMAX = SMOY+SALT
      SEQ  = SMAX*(1.-R)**VALEUR(1,4)
      ENDOM = VALEUR(1,1)-VALEUR(1,2)*LOG10(SEQ-VALEUR(1,3))
      ENDOM = 10**ENDOM
      ENDOM = 1./ENDOM
C
      RETURN
      END
C --------------------------------------------------
      SUBROUTINE ENDOL4(SMOY,SALT,VALEUR,RIQF,ENDOM)
C --------------------------------------------------
C CALCUL DE L'ENDOMMAGEMENT D'UN CYCLE (SMOY,SALT) PAR L'ANCIENNE LOI 
C MANUEL
C ENTREES : SMOY        CONTRAINTE MOYENNE DU CYCLE
C           SALT        CONTRAINTE ALTERNEE DU CYCLE
C           VALEUR      COEFFICIENTS MATERIAUX
C           RIQF        IQF          
C SORTIES : ENDOM       ENDOMMAGEMENT
C ***********************************
      PARAMETER(MAXCAL=60)
C
      REAL VALEUR(MAXCAL,*)
C
      EPS   = 1E-6
C
      X1  = -.7692
      Y1  = .7692
      X2  = 1.0135
      Y2  = .3378
      D1  = X1/Y1
      D2  = X2/Y2
C
      R = SMOY/SALT
C
C POSITIONNEMENT DANS LE DIAGRAMME DE HAIG
C ----------------------------------------
      IF(R.LE.D1) THEN
        C1 = 0.
        C2 = 0.
      ELSE IF((D1.LE.R).AND.(R.LE.D2)) THEN
        C1 = 1.715
        C2 = .415
      ELSE
        C1 = 2.96
        C2 = 0.
      ENDIF
C
      SIG01  = C1*SALT+C2*SMOY
      IF(ABS(SIG01).LE.EPS) THEN
        ENDOM = 0.
      ELSE
        ENDOM = 1E-5*(SIG01/RIQF)**(-VALEUR(1,1))
      ENDIF
C
      RETURN
      END
C ---------------------------------------------------------------------
      SUBROUTINE ENDOR(NOMPB,SMOY,SALT,SMOYRE,NRE,SALTRE,NPTREX,NPTREY,
     &                 IU,ENDOM)
C ------------------------------
C CALCUL DE L'ENDOMMAGEMENT D'UN CYCLE (SMOY,SALT) PAR RESEAU
C ENTREES : NOMPB                    NOM DU PROBLEME
C           SMOY                     CONTRAINTE MOYENNE DU CYCLE
C           SALT                     CONTRAINTE ALTERNEE DU CYCLE
C           SMOYRE,NRE,SALTRE        RESEAU Smoy,N,Salt
C           NPTREX                   NOMBRE DE POINTS RESEAU Smoy
C           NPTREY                   NOMBRE DE POINTS RESEAU N
C           IU                       UNITE DU FICHIER prefixe.erreurs
C SORTIES : ENDOM                    ENDOMMAGEMENT
C ************************************************
      PARAMETER(MAXCAL=60,NRESX=5,NRESY=9)
C
      CHARACTER*80  MESSAG
      CHARACTER*80 NOMPB
      REAL SMOYRE(MAXCAL,*),NRE(MAXCAL,*),SALTRE(MAXCAL,NRESX,*),
     &     X(NRESY),Y(NRESY),Z(NRESY),W(NRESY)
      INTEGER NPTREX(*),NPTREY(*)
C
C CALCUL DE N POUR LA LOI RESEAU
C ------------------------------
      IF(SMOY.LT.SMOYRE(1,1)) THEN
        WRITE(MESSAG,'(A)') ' Smoy < SmoyREmin'
        CALL ERROR(NOMPB,MESSAG,IU)
      ELSEIF(SMOY.GT.SMOYRE(1,NPTREX(1))) THEN
        WRITE(MESSAG,'(A)') ' Smoy > SmoyREmax'
        CALL ERROR(NOMPB,MESSAG,IU)
      ELSE
        DO J=1,NPTREY(1)
          DO I=1,NPTREX(1)
            X(I) = SMOYRE(1,I)
            Y(I) = SALTRE(1,I,J)
          ENDDO
          Z(J) = FINT(NOMPB,'Smoy',SMOY,X,Y,IU,NPTREX(1),1,0)
          W(NPTREY(1)-J+1) = Z(J)
        ENDDO
C
        IF(SALT.GT.Z(1)) THEN
          WRITE(6,*) 'RUPTURE ATTEINTE POUR SMOY =',SMOY
          WRITE(6,*) '                      SALT =',SALT
          WRITE(MESSAG,'(A)') ' Salt > SaltREmax'
          CALL ERROR(NOMPB,MESSAG,IU)
        ELSEIF(SALT.LT.Z(NPTREY(1))) THEN
          ENDOM = 0.
        ELSE
          DO J=1,NPTREY(1)
            X(NPTREY(1)-J+1) = LOG10(NRE(1,J))
          ENDDO
          IF(NPTREY(1).EQ.2) THEN
            KINT = 1
          ELSE
            KINT = 2
          ENDIF
          XN    = FINT(NOMPB,'Salt',SALT,W,X,IU,NPTREY(1),KINT,0)
          XN    = 10**XN
          ENDOM = 1./XN
        ENDIF
      ENDIF
C
      RETURN
      END
C ---------------------------------------
      FUNCTION FSEUIL(S1,S2,OMEGA,SIGMAD)
C ---------------------------------------
C     WRITE(6,*) 'FONCTION FSEUIL'
C
      RT     = 4.13*SIGMAD
      RC     = -RT
      RT     = OMEGA*RT
      RC     = OMEGA*RC
      SIGMAP = OMEGA*SIGMAD
      SIGMAM = RT*(SIGMAP+RC)/(SIGMAP+RT)
C
      SMOY   = .5*(S1+S2)
      IF((RC.LT.SMOY).AND.(SMOY.LT.SIGMAM)) THEN
        FSEUIL = ABS(2.*(SMOY-RC))
      ELSEIF((SIGMAM.LE.SMOY).AND.(SMOY.LT.RT)) THEN
        FSEUIL = ABS(2.*SIGMAP*(1.-SMOY/RT))
      ELSE
        FSEUIL = 0.
      ENDIF
C
      RETURN
      END
C -----------------------------------
      SUBROUTINE INIECR(NVOL,I,IECRI)
C -----------------------------------
C INITIALISATION POUR LES ECRITURES
C ENTREES : NVOL   NUMERO DES VOLS EDITES
C           I      NUMERO DE LA BOUCLE SUR LES VOLS
C SORTIES : IECRI  =0 PAS D'ECRITURE =1 ECRITURE
C **********************************************
      INTEGER NVOL(*)
C
      IECRI  = 0
      DO J=1,5
        IF(I.EQ.NVOL(J)) IECRI  = 1
      ENDDO 
C
      RETURN
      END
C -----------------------------------------------------------------
      SUBROUTINE INIEND(ENDTOT,SMINS,SMAXS,ENVOL,EFVOL,IMINS,IMAXS,
     &                  ISEVS,ISEVP,NTOT)
C ---------------------------------------
C INITIALISATION DES DONNEES D'ENDOMMAGEMENT
C SORTIES : ENDTOT      CUMUL DE L'ENDOMMAGEMENT
C           SMINS,SMAXS CONTRAINTES MIN ET MAX SUR LA SEQUENCE
C           ENVOL       ENDOMAGEMENT PAR VOL
C           EFVOL       EFFICACITE PAR VOL
C           IMINS,IMAXS VOLS CONTENANT LES CONTRAINTES MIN ET MAX
C           ISEVS       VOL LE PLUS SEVERE RAIN-FLOW
C           ISEVP       VOL LE PLUS SEVERE PREFFAS
C           NTOT        NOMBRE DE CONTRAINTES TOTALES
C ***************************************************
      WRITE(6,*) 'SOUS PROGRAMME INIEND'
C
      ENDTOT = 0.
      SMINS  = 1E6
      SMAXS  = -1E6
      ENVOL  = 0.
      EFVOL  = 0.
      IMINS  = 0
      IMAXS  = IMINS
      ISEVS  = IMINS
      ISEVP  = IMINS
      NTOT   = 0
C
      RETURN
      END
C ---------------------------------------------------------------------
      SUBROUTINE INIPRE(SMINS,SMAXS,A,B,C,R02,RM,ICOMP,SMINSV,HMA,HMI,
     &                  HO,VMICL,NCOM)
C ------------------------------------
C INITIALISATION DES DONNEES PREFFAS
C ENTREES : SMINS         CONTRAINTE MINIMALE SUR LA SEQUENCE
C           SMAXS         CONTRAINTE MAXIMALE SUR LA SEQUENCE
C           A,B,C,R02,RM  PARAMETRES DE LA LOI D'ELBER
C           ICOMP         =1 COMPRESSION =2 PAS DE COMPRESSION PREFFAS
C SORTIES : SMINSV        CONTRAINTE MINIMALE EVENTUELLEMENT MODIFIEE
C           HMA,HMI,HO    PARAMETRES D'HISTOIRE
C           VMICL         VALEUR MINIMALE POUR LE CUMUL LINEAIRE
C           NCOM          COMPTEUR DE CYCLES D' HISTOIRE
C ******************************************************
      REAL HMA(*),HMI(*),HO(*)
C
      WRITE(6,*) 'SOUS PROGRAMME INIPRE'
C
      IF((ICOMP.EQ.2).AND.(SMINS.LT.0.)) THEN
        SMINSV = 0.
      ELSE
        SMINSV = SMINS
      ENDIF
      HMA(1) = SMAXS
      HMI(1) = SMINSV
      IF(SMINSV.LT.0.) THEN
        G     = (1.-A)/A
        HO(1) = SMAXS*(1.-A*(1.+2.*G*ABS(SMINSV)/(R02+RM)))
        VMICL = 0.
      ELSE
        R      = SMINSV/SMAXS
        U      = A+B*R+C*R**2
        HO(1) = SMAXS-U*(SMAXS-SMINSV)
        VMICL = SMINSV
      ENDIF  
      NCOM = 1
C
      RETURN
      END
C ----------------------------------------------------
      SUBROUTINE LECDEB(NOMPB,TITSEQ,ITYPE,NBVOL,ILIG)			
C ----------------------------------------------------
C LECTURE DES DONNEES GLOBALES 
C ENTREES : NOMPB  NOM DU PROBLEME
C SORTIES : TITSEQ TITRE DE LA SEQUENCE
C           ITYPE  TYPE DE SPECTRE EN ENTREE
C           NBVOL  NOMBRE DE VOLS
C           ILIG   NUMERO DE LIGNE
C ********************************
      CHARACTER*80  MESSAG

	CHARACTER*80 NOMPB

	CHARACTER*(*) TITSEQ
      CHARACTER*110 CARTE
C
      WRITE(6,*) 'SOUS PROGRAMME LECDEB'
C
C LECTURE DU FICHIER SPECTRE
C --------------------------
      REWIND(2)
      ILIG   = 1
C
C LECTURE DU TYPE DE SPECTRE (SIMPLIFIE,PALIER,COMPLEXE)
C ------------------------------------------------------
      READ(2,'(A)',ERR=900) CARTE
      IF(INDEX(CARTE,'SIMPLIFIE').NE.0) ITYPE = 1
      IF(INDEX(CARTE,'PALIER').NE.0)    ITYPE = 2
      IF(INDEX(CARTE,'COMPLEXE').NE.0)  ITYPE = 3
      WRITE(6,*) 'ITYPE =',ITYPE
      ILIG = ILIG+1
C       
      IF((ITYPE.EQ.1).OR.(ITYPE.EQ.2)) THEN
C
C LECTURE DU TITRE DE LA SEQUENCE
C -------------------------------
        READ(2,'(A)',ERR=900) CARTE
        L1 = INDEX(CARTE,'"')
        L2 = INDEX(CARTE(L1+1:80),'"')
        L3 = INDEX(CARTE(L1+L2+1:80),'"')
        L4 = INDEX(CARTE(L1+L2+L3+1:80),'"')
        READ(CARTE(L1+L2+L3+1:L1+L2+L3+L4-1),'(A)') TITSEQ
        ILIG  = ILIG+1
        NBVOL = 1
C       
      ELSEIF(ITYPE.EQ.3) THEN
C
C LECTURE DU TITRE DE LA SEQUENCE
C -------------------------------
        READ(2,'(A)',ERR=900) TITSEQ
        WRITE(6,'(A)') TITSEQ
        ILIG = ILIG+1
C       
C LECTURE DU NOMBRE DE VOLS
C -------------------------
        READ(2,'(A)',ERR=900) CARTE
        ILIG = ILIG+1
        READ(2,*,ERR=900) NBVOL
        ILIG = ILIG+1
      ENDIF
      RETURN
C
  901 FORMAT(1X,'ERREUR A LA LECTURE DU FICHIER SPECTRE LIGNE= ',I2)
  900 WRITE(MESSAG,901) ILIG
      CALL ERROR(NOMPB,MESSAG,19)
      END
C ------------------------------------------------------------------
      SUBROUTINE LECDON(NOMPB,TITRE,AMAT,VALEUR,XM,RIQF,CE,A,B,C,XN,
     &                  R02,RM,SIGMAD,SMOYRE,NRE,SALTRE,OMEGA,NPTREX,
     &                  NPTREY,NVOL,NBVOL,ICAL,IPAP,ILOI,KSNUL,ITYPE,
     &                  ICOMP,ISTOC)
C ----------------------------------
C LECTURE DU FICHIER STRATEGIE
C ENTREES : NOMPB              NOM DU PROBLEME
C SORTIES : TITRE              TITRE DU PROBLEME
C           AMAT               MATERIAU
C           VALEUR             VALEURS MATERIAUX
C           XM                 COEFFICIENT MULTIPLICATIF
C           RIQF               COEFFICIENT IQF
C           CE,A,B,C,R02,RM,XN COEFFICIENT LOI ELBER
C           SIGMAD             LIMITE D'ENDURANCE
C           SMOYRE,NRE,SALTRE  RESEAU Smoy,N,Salt
C           OMEGA              VALEUR DE FILTRAGE
C           NPTREX             NOMBRE DE POINTS Smoy RESEAU
C           NPTREY             NOMBRE DE POINTS N RESEAU
C           NVOL               NUMERO DES VOLS EDITES
C           NBVOL              NOMBRE DE VOLS
C           ICAL               =1 AMORCAGE =2 PROPAGATION =3 AMORCAGE+PROPAGATION
C           IPAP               =1 TRANSFORMATION EN NIVEAUX =2 NON
C           ILOI               LOI UTILISEE
C           KSNUL              PARAMETRE D'ELIMINATION DES CYCLES
C           ITYPE              TYPE DE SPECTRE EN ENTREE
C           ICOMP              =1 COMPRESSION =2 PAS DE COMPRESSION PREFFAS
C           ISTOC              =1 PAS DE STOCKAGE DU FICHIER FILTRE =2 STOCKAGE
C *****************************************************************************
      PARAMETER(MAXCAL=60,NRESX=5,NRESY=9)
C
      CHARACTER*80 NOMPB

	CHARACTER*(*) TITRE,AMAT
      CHARACTER*80  CARTE,CHAMAT,CHAINE(100),CHAINS(100)
      REAL VALEUR(MAXCAL,*),SMOYRE(MAXCAL,*),NRE(MAXCAL,*),
     &     SALTRE(MAXCAL,NRESX,*),VALSTR(100)
      INTEGER NVOL(*),NPTREX(*),NPTREY(*)
C
      WRITE(6,*) 'SOUS PROGRAMME LECDON'
C
      REWIND(2)
      WRITE(AMAT,'(A)') 'HORS DATABASE'
C
C CHARGEMENT DES CHAINES 
C ----------------------
      CHAINS(1)  = '%NBVOL'
      CHAINS(2)  = '%LOI'
      CHAINS(3)  = '%XM'
      CHAINS(4)  = '%KSNUL'
      CHAINS(5)  = '%IQF'
      CHAINS(6)  = '%VOL1'
      CHAINS(7)  = '%VOL2'
      CHAINS(8)  = '%VOL3'
      CHAINS(9)  = '%VOL4'
      CHAINS(10) = '%VOL5'
      CHAINS(11) = '%TYPE'
      CHAINS(12) = '%PAP'
      CHAINS(13) = '%PREF'
      CHAINS(14) = '%OMEGA'
      CHAINS(15) = '%STOC'
C
C LECTURE DU TYPE DE SPECTRE (SIMPLIFIE,PALIER,COMPLEXE)
C ------------------------------------------------------
      CHAMAT = '%SPECTYP'
      CALL LECCHA(NOMPB,CHAMAT,2,19,CARTE)
      IF(INDEX(CARTE,'SIMPLIFIE').NE.0) ITYPE = 1
      IF(INDEX(CARTE,'PALIER').NE.0)    ITYPE = 2
      IF(INDEX(CARTE,'COMPLEXE').NE.0)  ITYPE = 3
      WRITE(6,*) 'ITYPE =',ITYPE
C
C LECTURE DU TITRE
C ----------------
      CHAMAT = '%TITRE'
      CALL LECCHA(NOMPB,CHAMAT,2,19,TITRE)
C
C LECTURE DES MATERIAUX ET DES VALEURS
C ------------------------------------
      CHAMAT = '%NOMMAT'
      CALL LECCHA(NOMPB,CHAMAT,2,19,AMAT)
      CALL LECSTR(NOMPB,CHAINS,2,19,15,VALSTR)
C
      NBVOL   = VALSTR(1)
      ILOI    = VALSTR(2)
      XM      = VALSTR(3)  
      KSNUL   = VALSTR(4)
      RIQF    = VALSTR(5)
      NVOL(1) = VALSTR(6)
      NVOL(2) = VALSTR(7)
      NVOL(3) = VALSTR(8)
      NVOL(4) = VALSTR(9)
      NVOL(5) = VALSTR(10)
      ICAL    = VALSTR(11)
      IPAP    = VALSTR(12)
      ICOMP   = VALSTR(13)
      OMEGA   = VALSTR(14)
      ISTOC   = VALSTR(15)
C
      WRITE(6,*) 'NOMBRE DE VOLS              =',NBVOL
      WRITE(6,*) 'TYPE DE CALCUL              =',ICAL
      WRITE(6,*) '0=PAS DE CALCUL 1=AMORCAGE 2=PROPAGATION 3=AMORCAGE+PR
     &OPAGATION'
      WRITE(6,*) 'LOI UTILISEE                =',ILOI
      WRITE(6,*) '1=RESEAU 2=ANALYTIQUE'
      WRITE(6,*) 'COEFFICIENT MULTIPLICATIF   =',XM
      WRITE(6,*) 'PARAMETRE D''ELIMINATION     =',KSNUL
      WRITE(6,*) 'IQF                         =',RIQF 
      WRITE(6,*) 'PASSAGE EN NIVEAUX          =',IPAP
      WRITE(6,*) '1=OUI 2=NON'
      WRITE(6,*) 'PRISE EN COMPTE COMPRESSION =',ICOMP
      WRITE(6,*) 'OMEGA                       =',OMEGA
      WRITE(6,*) 'STOCKAGE DU FICHIER FILTRE  =',ISTOC
      WRITE(6,*) '1=NON 2=OUI'
      WRITE(6,'(1X,A,1X,A)') 'MATERIAU =',
     &                        AMAT(IFDEB(AMAT):IFFIN(AMAT))
C
      IF(ILOI.EQ.1) THEN
C
C CHARGEMENT DES CHAINES A CHERCHER
C ---------------------------------
        L = 0
        DO K=1,NRESX
          L = L+1
          WRITE(CHAINE(L),'(A7,I1)') '%SMOYRE',K
        ENDDO
        DO K=1,NRESY
          L = L+1
          WRITE(CHAINE(L),'(A4,I1)') '%NRE',K
        ENDDO
        DO J=1,NRESY
          DO K=1,NRESX
            L = L+1
            WRITE(CHAINE(L),'(A7,I1,I1)') '%SALTRE',J,K
          ENDDO
        ENDDO
        CHAINE(L+1) = '%ELCE'
        CHAINE(L+2) = '%ELA'
        CHAINE(L+3) = '%ELB'
        CHAINE(L+4) = '%ELC'
        CHAINE(L+5) = '%ELN'
        CHAINE(L+6) = '%LIEL'
        CHAINE(L+7) = '%RM'
        CHAINE(L+8) = '%SD'
        NBMAT       = L+8
        CALL LECMAT(NOMPB,CHAINE,AMAT,8,19,1,NBMAT,VALEUR) 
        CALL RESEAU(NOMPB,VALEUR,1,0,SMOYRE,NRE,SALTRE,NPTREX,NPTREY)
        CE     = VALEUR(1,NBMAT-7)
        A      = VALEUR(1,NBMAT-6)
        B      = VALEUR(1,NBMAT-5)
        C      = VALEUR(1,NBMAT-4)
        XN     = VALEUR(1,NBMAT-3)
        R02    = VALEUR(1,NBMAT-2)
        RM     = VALEUR(1,NBMAT-1)
        SIGMAD = VALEUR(1,NBMAT)
C
      ELSEIF(ILOI.EQ.2) THEN
        NBMAT      = 10
        CHAINE(1)  = '%MANP'
        CHAINE(2)  = '%MANQ'
        CHAINE(3)  = '%ELCE'
        CHAINE(4)  = '%ELA'
        CHAINE(5)  = '%ELB'
        CHAINE(6)  = '%ELC'
        CHAINE(7)  = '%ELN'
        CHAINE(8)  = '%LIEL'
        CHAINE(9)  = '%RM'
        CHAINE(10) = '%SD'
        CALL LECMAT(NOMPB,CHAINE,AMAT,8,19,1,NBMAT,VALEUR) 
        CE     = VALEUR(1,3)
        A      = VALEUR(1,4)
        B      = VALEUR(1,5)
        C      = VALEUR(1,6)
        XN     = VALEUR(1,7)
        R02    = VALEUR(1,8)
        RM     = VALEUR(1,9)
        SIGMAD = VALEUR(1,10)
C
      ELSEIF(ILOI.EQ.3) THEN
        NBMAT     = 9
        CHAINE(1) = '%MANP'
        CHAINE(2) = '%ELCE'
        CHAINE(3) = '%ELA'
        CHAINE(4) = '%ELB'
        CHAINE(5) = '%ELC'
        CHAINE(6) = '%ELN'
        CHAINE(7) = '%LIEL'
        CHAINE(8) = '%RM'
        CHAINE(9) = '%SD'
        CALL LECMAT(NOMPB,CHAINE,AMAT,8,19,1,NBMAT,VALEUR) 
        CE     = VALEUR(1,2)
        A      = VALEUR(1,3)
        B      = VALEUR(1,4)
        C      = VALEUR(1,5)
        XN     = VALEUR(1,6)
        R02    = VALEUR(1,7)
        RM     = VALEUR(1,8)
        SIGMAD = VALEUR(1,9)
      ENDIF
      WRITE(6,*) 'Ceff =',CE
      WRITE(6,*) 'A    =',A
      WRITE(6,*) 'B    =',B
      WRITE(6,*) 'm    =',XN
      WRITE(6,*) 'RO2  =',R02
      WRITE(6,*) 'Rm   =',RM
      WRITE(6,*) 'Sd   =',SIGMAD
C
      RETURN
      END
C ----------------------------------------------------------------
      SUBROUTINE LECPAL(NOMPB,DESIG,CEQUI,DELSI,ISEQ,IFREQ,IEXIST,
     &                  NBPALR,NBSEQ)
C -----------------------------------
C LECTURE DU SPECTRE PALIER
C ENTREES : NOMPB   NOM DU PROBLEME
C           NBVOL   NOMBRE DE VOLS
C SORTIES : DESIG   DESIGNATION DES PALIERS
C           CEQUI   CONTRAINTES D'EQUILIBRE
C           DELSI   DELTA SIGMA
C           ISEQ    SEQUENCE   
C           IFREQ   FREQUENCES
C           IEXIST  =1 VOL EXISTANT
C           NBPALR  NOMBRE DE PALIERS REELS
C           NBSEQ   LONGUEUR DE LA SEQUENCE 
C *****************************************
      PARAMETER(NBPAL=20,NBVOLM=100)
C
      CHARACTER*80 NOMPB

	CHARACTER*(*) DESIG(NBPAL,*)
      CHARACTER*80 CARTE,SEQ(NBVOLM),CHAINE(4),MESSAG
      REAL CEQUI(NBPAL,*),DELSI(NBPAL,*)
      INTEGER IFREQ(NBPAL,*),ISEQ(NBVOLM,*),IEXIST(*),NBPALR(*)
C
      WRITE(6,*) 'SOUS PROGRAMME LECPAL'
C
      REWIND(2)
      NBSEQ  = 0
      ISTOCK = 0
      DO I=1,NBVOLM
        IEXIST(I) = 0
      ENDDO
      WRITE(CHAINE(1),'(A6)') '%DESIG'
      WRITE(CHAINE(2),'(A6)') '%CEQUI'
      WRITE(CHAINE(3),'(A6)') '%DELSI'
      WRITE(CHAINE(4),'(A6)') '%FREQU'
C
    2 READ(2,'(A)',END=3,ERR=900) CARTE
C
C LECTURE DES LIBELLES DES PALIERS
C --------------------------------
      IF((INDEX(CARTE,'ABREFIX').NE.0).AND.(INDEX(CARTE,'%DESIG').NE.0))
     & THEN
        L1 = INDEX(CARTE,'"')
        L2 = INDEX(CARTE(L1+1:80),'"')
        L3 = INDEX(CARTE(L1+L2+1:80),'"')
        L4 = INDEX(CARTE(L1+L2+L3+1:80),'"')
        LI = IFDEB(CARTE(L1+L2+L3+1:L1+L2+L3+L4-1))
        IF(LI.NE.0) THEN
          READ(CARTE(L1+LEN('%DESIG')+1:L1+L2-1),*,ERR=900) I
          IF(I.NE.ISTOCK) NBPALR(I) = 0
          ISTOCK    = I
          IEXIST(I) = 1
          NBPALR(I) = NBPALR(I)+1
          READ(CARTE(L1+L2+L3+1:L1+L2+L3+L4-1),'(A13)') 
     &      DESIG(NBPALR(I),I)
        ENDIF
      ENDIF  
C
C LECTURE DES CONTRAINTES D'EQUILIBRE
C -----------------------------------
      IF((INDEX(CARTE,'ABREFIX').NE.0).AND.(INDEX(CARTE,'%CEQUI').NE.0))
     & THEN
        L1 = INDEX(CARTE,'"')
        L2 = INDEX(CARTE(L1+1:80),'"')
        L3 = INDEX(CARTE(L1+L2+1:80),'"')
        L4 = INDEX(CARTE(L1+L2+L3+1:80),'"')
        LI = IFDEB(CARTE(L1+L2+L3+1:L1+L2+L3+L4-1))
        IF(LI.NE.0) THEN
          READ(CARTE(L1+LEN('%CEQUI')+1:L1+L2-1),*,ERR=900) I
          READ(CARTE(L1+L2+L3+1:L1+L2+L3+L4-1),*) CEQUI(NBPALR(I),I)
        ENDIF
      ENDIF  
C
C LECTURE DES DELTA SIGMA
C -----------------------
      IF((INDEX(CARTE,'ABREFIX').NE.0).AND.(INDEX(CARTE,'%DELSI').NE.0))
     & THEN
        L1 = INDEX(CARTE,'"')
        L2 = INDEX(CARTE(L1+1:80),'"')
        L3 = INDEX(CARTE(L1+L2+1:80),'"')
        L4 = INDEX(CARTE(L1+L2+L3+1:80),'"')
        LI = IFDEB(CARTE(L1+L2+L3+1:L1+L2+L3+L4-1))
        IF(LI.NE.0) THEN
          READ(CARTE(L1+LEN('%DELSI')+1:L1+L2-1),*,ERR=900) I
          READ(CARTE(L1+L2+L3+1:L1+L2+L3+L4-1),*) DELSI(NBPALR(I),I)
        ENDIF
      ENDIF  
C
C LECTURE DES FREQUENCES
C ----------------------
      IF((INDEX(CARTE,'ABREFIX').NE.0).AND.(INDEX(CARTE,'%FREQU').NE.0))
     & THEN
        L1 = INDEX(CARTE,'"')
        L2 = INDEX(CARTE(L1+1:80),'"')
        L3 = INDEX(CARTE(L1+L2+1:80),'"')
        L4 = INDEX(CARTE(L1+L2+L3+1:80),'"')
        LI = IFDEB(CARTE(L1+L2+L3+1:L1+L2+L3+L4-1))
        IF(LI.NE.0) THEN
          READ(CARTE(L1+LEN('%FREQU')+1:L1+L2-1),*,ERR=900) I
          READ(CARTE(L1+L2+L3+1:L1+L2+L3+L4-1),*) IFREQ(NBPALR(I),I)
C         WRITE(6,*) I,NBPALR(I),CEQUI(NBPALR(I),I),DELSI(NBPALR(I),I),
C    &               IFREQ(NBPALR(I),I)
        ENDIF
      ENDIF  
C
C LECTURE DE LA SEQUENCE
C ----------------------
      IF((INDEX(CARTE,'ABREFIX').NE.0).AND.(INDEX(CARTE,'%SEQ').NE.0))
     & THEN
        L1 = INDEX(CARTE,'"')
        L2 = INDEX(CARTE(L1+1:80),'"')
        L3 = INDEX(CARTE(L1+L2+1:80),'"')
        L4 = INDEX(CARTE(L1+L2+L3+1:80),'"')
        LI = IFDEB(CARTE(L1+L2+L3+1:L1+L2+L3+L4-1))
        IF(LI.NE.0) THEN
          READ(CARTE(L1+LEN('%SEQ')+1:L1+L2-1),*,ERR=900) I  
          NBSEQ = NBSEQ+1
          READ(CARTE(L1+L2+L3+1:L1+L2+L3+L4-1),'(A)',ERR=900) SEQ(I)
          L     = INDEX(SEQ(I),'-')
          READ(SEQ(I)(IFDEB(SEQ(I)):L-1),*) ISEQ(I,1)
          READ(SEQ(I)(L+1:80),*)            ISEQ(I,2)
        ENDIF
      ENDIF
      GOTO 2
    3 CONTINUE
      RETURN
C
  901 FORMAT(1X,'ERREUR A LA LECTURE DU FICHIER prefixe.sigma')
  900 WRITE(MESSAG,901)
      CALL ERROR(NOMPB,MESSAG,19)
      END
C ----------------------------------------------------------------------
      SUBROUTINE LECSIM(NOMPB,SIGSIM,ISEQ,IEXIST,NPICS,NVOL,NBSEQ,NBVOL)
C ----------------------------------------------------------------------
C LECTURE DU SPECTRE SIMPLIFIE
C ENTREES : NOMPB   NOM DU PROBLEME
C SORTIES : SIGSIM  CONTRAINTES
C           ISEQ    SEQUENCE
C           IEXIST  =1 VOL EXISTANT
C           NPICS   NOMBRE DE PICS PAR VOL
C           NVOL    NUMERO DES VOLS EDITES
C           NBSEQ   LONGUEUR DE LA SEQUENCE
C           NBVOL   NOMBRE DE VOLS
C ********************************
      PARAMETER(MAXSIG=800000,NBVOLM=100)
C
      CHARACTER*80 NOMPB
      CHARACTER*80  SEQ(NBVOLM)
      CHARACTER*80 CARTE,CHAINE,MESSAG
      REAL SIGSIM(MAXSIG/1000,*)
      INTEGER ISEQ(NBVOLM,*),NPICS(*),IEXIST(*),NVOL(*),MVOL(5)
C
      WRITE(6,*) 'SOUS PROGRAMME LECSIM'
      ISTOCK = 0
      DO I=1,NBVOLM
        IEXIST(I) = 0
      ENDDO
      DO I=1,5
        MVOL(I) = 0
      ENDDO
      WRITE(CHAINE,'(A6)') '%SIGMA'
C
      REWIND(2)
    2 READ(2,'(A)',END=3,ERR=900) CARTE
C
C LECTURE DES CONTRAINTES
C -----------------------
      IF((INDEX(CARTE,'ABREFIX').NE.0).AND.
     &   (INDEX(CARTE,CHAINE(IFDEB(CHAINE):IFFIN(CHAINE))).NE.0))
     & THEN
        L1 = INDEX(CARTE,'"')
        L2 = INDEX(CARTE(L1+1:80),'"')
        L3 = INDEX(CARTE(L1+L2+1:80),'"')
        L4 = INDEX(CARTE(L1+L2+L3+1:80),'"')
        LI = IFDEB(CARTE(L1+L2+L3+1:L1+L2+L3+L4-1))
        IF(LI.NE.0) THEN
          READ(CARTE(L1+LEN('%SIGMA')+1:L1+L2-1),*,ERR=900) I
          IF(I.NE.ISTOCK) NPICS(I) = 0
          IEXIST(I) = 1
          NPICS(I)  = NPICS(I)+1
          ISTOCK    = I
          READ(CARTE(L1+L2+L3+1:L1+L2+L3+L4-1),*) SIGSIM(NPICS(I),I)
        ENDIF
      ENDIF  
C
C LECTURE DE LA SEQUENCE
C ----------------------
      IF((INDEX(CARTE,'ABREFIX').NE.0).AND.(INDEX(CARTE,'%SEQ').NE.0))
     & THEN
        L1 = INDEX(CARTE,'"')
        L2 = INDEX(CARTE(L1+1:80),'"')
        L3 = INDEX(CARTE(L1+L2+1:80),'"')
        L4 = INDEX(CARTE(L1+L2+L3+1:80),'"')
        LI = IFDEB(CARTE(L1+L2+L3+1:L1+L2+L3+L4-1))
        IF(LI.NE.0) THEN
          READ(CARTE(L1+LEN('%SEQ')+1:L1+L2-1),*,ERR=900) I  
          NBSEQ = NBSEQ+1
          READ(CARTE(L1+L2+L3+1:L1+L2+L3+L4-1),'(A)',ERR=900) SEQ(I)
          L     = INDEX(SEQ(I),'-')
          READ(SEQ(I)(IFDEB(SEQ(I)):L-1),*) ISEQ(I,1)
          READ(SEQ(I)(L+1:80),*)            ISEQ(I,2)
        ENDIF
      ENDIF
      GOTO 2
    3 CONTINUE
C
      REWIND(2)
      IVOL = 0
      DO N=1,NBSEQ    
C
C ECRITURE
C --------
        DO L1=1,ISEQ(N,1)
          IVOL = IVOL+1
          IF(IEXIST(ISEQ(N,2)).EQ.1) THEN
            NPIC = NPICS(ISEQ(N,2))
          ELSE
            NPIC = 0
          ENDIF
          DO J=1,5
            IF((MVOL(J).EQ.0).AND.(NVOL(J).EQ.ISEQ(N,2))) MVOL(J) = IVOL
          ENDDO
C
          NTOT = NTOT+NPIC
          IF(NPIC.GT.MAXSIG) THEN
  100       FORMAT(1X,'ERREUR - NPIC > 100000')
            WRITE(MESSAG,100)
            CALL ERROR(NOMPB,MESSAG,19)
          ENDIF
C
  110     FORMAT('VOL NO ',I4)
  120     FORMAT('NOMBRE DE VALEURS')
  130     FORMAT('SUITE DE VALEURS')
          WRITE(2,110) IVOL
          WRITE(2,120) 
          WRITE(2,*)   NPIC
          WRITE(2,130) 
          K     = -10
          NREST = NPIC-NPIC/10*10
          DO I=1,NPIC/10
            K = K+10
            WRITE(2,'(10(F10.5,1X))') (SIGSIM(J+K,ISEQ(N,2)),J=1,10)
          ENDDO
          IF(NREST.GE.1) WRITE(2,'(10(F10.5,1X))') 
     &      (SIGSIM(NPIC/10*10+L,ISEQ(N,2)),L=1,NPIC-NPIC/10*10)
        ENDDO  
      ENDDO   
C
      NBVOL = IVOL
      DO I=1,5
        NVOL(I) = MVOL(I)
      ENDDO
      RETURN
C
  901 FORMAT(1X,'ERREUR A LA LECTURE DU FICHIER prefixe.sigma')
  900 WRITE(MESSAG,901)
      CALL ERROR(NOMPB,MESSAG,19)
      END
C -----------------------------------------------------------------
      SUBROUTINE LECSPE(NOMPB,NUVOL,XM,IU,IR,ILIG,SIG,XI,NPIC,NTOT)	
C -----------------------------------------------------------------
C LECTURE DES SPECTRES
C ENTREES : NOMPB  NOM DU PROBLEME
C           NUVOL  NUMERO DU VOL
C           XM     COEFFICIENT MULTIPLICATIF DES CONTRAINTES
C           ITYPE  TYPE DE SPECTRE
C           IU     UNITE DU FICHIER SIGMA
C           IR     =1 REWIND
C           ILIG   NUMERO DE LIGNE
C SORTIES : SIG    CONTRAINTES LUES
C           XI     TEMPS
C           NPIC   NOMBRE DE CONTRAINTES
C           NTOT   NOMBRE DE CONTRAINTES TOTALES
C **********************************************
      PARAMETER(MAXSIG=800000)
C
      CHARACTER*80 NOMPB
      CHARACTER*110 CARTE
ccc   CHARACTER*80  MESSAG,CHAINE
      CHARACTER*80  MESSAG
      CHARACTER*4   CNUVOL
      REAL SIG(*),XI(*)
C
C      WRITE(6,*) 'SOUS PROGRAMME LECSPE'
C
C LECTURE DU FICHIER
C ------------------
      IF(IR.EQ.1) THEN
        REWIND(IU)
        ILIG = 1
      ENDIF
C      WRITE(6,*) 'VOL NUMERO =',NUVOL
C
C POSITIONNEMENT SUR LE VOL
C -------------------------
   90 FORMAT('VOL NO ',I4)
      
ccc   WRITE(CHAINE,90) NUVOL
      WRITE(CNUVOL,'(I4)') NUVOL
    1 READ(IU,'(A)',ERR=900) CARTE
ccc   IF(INDEX(CARTE,CHAINE(IFDEB(CHAINE):IFFIN(CHAINE))).NE.0) GOTO 2
      IF   ((INDEX(CARTE,'VOL NO ').NE.0)
     & .AND.(INDEX(CARTE,CNUVOL).NE.0)) GOTO 2
      ILIG = ILIG+1
      GOTO 1
    2 CONTINUE
      BACKSPACE(IU)
      ILIG = ILIG-1
C
C LECTURE DU TITRE DU VOL
C -----------------------
      READ(IU,'(A)',ERR=900) CARTE
      ILIG = ILIG+1
C
C LECTURE DU NOMBRE DE VALEURS
C ----------------------------
      READ(IU,'(A)',ERR=900) CARTE
      ILIG = ILIG+1
      READ(IU,*,ERR=900) NPIC
      ILIG = ILIG+1
      NTOT = NTOT+NPIC
C
C LECTURE DES VALEURS
C -------------------
      READ(IU,'(A)',ERR=900) CARTE
      ILIG  = ILIG+1
      NREST = NPIC-NPIC/10*10
      K     = 0
      IF((NPIC.GT.0).AND.(NPIC.LT.10)) THEN
        READ(IU,'(10(F10.5,1X))',ERR=900) (SIG(J+K),J=1,NPIC)
        ILIG = ILIG+1
      ELSE
        DO I=1,NPIC/10
          READ(IU,'(10(F10.5,1X))',ERR=900) (SIG(J+K),J=1,10)
          ILIG = ILIG+1
          K    = K+10
        ENDDO
        IF(NREST.GE.1) THEN
          READ(IU,'(10(F10.5,1X))',ERR=900) (SIG(J+K),J=1,NREST)
          ILIG = ILIG+1
        ENDIF
      ENDIF  
C 
      IF(NPIC.GT.MAXSIG) THEN
  100   FORMAT(1X,'ERREUR - NPIC > 100000')
        WRITE(MESSAG,100)
        CALL ERROR(NOMPB,MESSAG,19)
      ENDIF
C     IF(NPIC.EQ.0) THEN
C 110   FORMAT(1X,'ERREUR - NPIC = 0')
C       WRITE(MESSAG,100)
C       CALL ERROR(NOMPB,MESSAG,19)
C     ENDIF
C
      DO I=1,NPIC
        XI(I)  = FLOAT(I)
        SIG(I) = XM*SIG(I)
      ENDDO
      RETURN
C
  901 FORMAT(1X,'ERREUR A LA LECTURE DU FICHIER SPECTRE LIGNE= ',I6)
  900 WRITE(MESSAG,901) ILIG
      CALL ERROR(NOMPB,MESSAG,19)
      END
C -------------------------------------------------------
      SUBROUTINE MINMAX(SIG,NPIC,SMAXV,SMINV,IMAXV,IMINV)
C -------------------------------------------------------
C CALCUL DES SIGMA MINIMAL ET MAXIMAL SUR LE VOL 
C ENTREES : SIG         CONTRAINTES DU VOL
C           NPIC        NOMBRE DE CONTRAINTES
C SORTIES : SMAXV,SMINV VALEURS MIN ET MAXI 
C           IMAXV,IMINV RANGS DES VALEURS MIN ET MAXI
C ***************************************************
      REAL SIG(*)
C
C      WRITE(6,*) 'SOUS PROGRAMME MINMAX'
C
      SMAXV = SIG(1)
      SMINV = SMAXV
      IMAXV = 1
      IMINV = IMAXV
      DO I=2,NPIC
        IF(SIG(I).LE.SMINV) THEN
          SMINV = SIG(I)
          IMINV = I
        ENDIF
        IF(SIG(I).GE.SMAXV) THEN
          SMAXV = SIG(I)
          IMAXV = I
        ENDIF
      ENDDO
C
      RETURN
      END
C --------------------------------------------------------------------
      SUBROUTINE MINER(NOMPB,SRF,VALEUR,RIQF,SMOYRE,NRE,SALTRE,NPTREX,
     &                 NPTREY,NCYCLE,ILOI,KSNUL,IECRI,NUMERO,ENDTOT)
C ------------------------------------------------------------------
C CUMUL LINEAIRE DE L'ENDOMMAGEMENT
C ENTREES : NOMPB             NOM DU PROBLEME           
C           SRF               CYCLES APRES RAIN-FLOW
C           VALEUR            VALEURS MATERIAUX
C           RIQF              IQF
C           SMOYRE,NRE,SALTRE RESEAU Smoy,N,Salt
C           NPTREX            NOMBRE DE POINTS RESEAU Smoy
C           NPTREY            NOMBRE DE POINTS RESEAU N
C           NCYCLE            NOMBRE DE CYCLES
C           ILOI              LOI UTILISEE
C           KSNUL             PARAMETRE D'ELIMINATION DES CYCLES
C           IECRI             FLAG D'ECRITURE
C           NUMERO            NUMERO DU VOL
C SORTIES : ENDTOT            CUMUL DES ENDOMMAGEMENTS
C*****************************************************
      PARAMETER(MAXCAL=60,NRESX=5)
C
      CHARACTER*80 NOMPB
      REAL SRF(2,*),SMOYRE(MAXCAL,*),NRE(MAXCAL,*),VALEUR(MAXCAL,*),
     &     SALTRE(MAXCAL,NRESX,*)
      INTEGER NPTREX(*),NPTREY(*)
C
      WRITE(6,*) 'SOUS PROGRAMME MINER'
C
   10 FORMAT(1X,':--------------------------------:----------------:----
     &----:-------:')
   20 FORMAT(1X,': ',A30,' : ',E14.6,' : ',A6,' : ',A5,' :') 
      SOMME = 0.
      DO I=1,NCYCLE
        IF(SRF(1,I).GE.0.) THEN
          IF((KSNUL.EQ.0).AND.(SRF(2,I).LE.0.)) SRF(2,I) = 0.
          SMOY = .5*(SRF(1,I)+SRF(2,I))
          SALT = .5*(SRF(1,I)-SRF(2,I))
          IF(ILOI.EQ.1) THEN
            CALL ENDOR(NOMPB,SMOY,SALT,SMOYRE,NRE,SALTRE,NPTREX,NPTREY,
     &                 19,ENDOM)
          ELSEIF(ILOI.EQ.2) THEN
            CALL ENDOL2(SMOY,SALT,VALEUR,RIQF,ENDOM)
          ELSEIF(ILOI.EQ.3) THEN
            CALL ENDOL4(SMOY,SALT,VALEUR,RIQF,ENDOM)
          ENDIF
          SOMME = SOMME+ENDOM
        ENDIF
      ENDDO
      WRITE(6,*) 'ENDOMMAGEMENT =',SOMME
      IF(IECRI.EQ.1) THEN
        WRITE(3,20) 'ENDOMMAGEMENT',SOMME,' ',' '
        WRITE(3,10) 
      ENDIF
C     
C SOMMATION DES ENDOMMAGEMENTS PAR VOL
C ------------------------------------
      ENDTOT = ENDTOT+SOMME
C
      RETURN
      END
C ---------------------------
      SUBROUTINE OPENF(NOMPB)
C ---------------------------
C OUVERTURE DES FICHIERS
C LES FICHIERS ONT POUR PREFIXE LE PREMIER ARGUMENT DE LA PROCEDURE
C SORTIES : NOMPB  FICHIER BANQUE
C *******************************
      CHARACTER*80 FIN,NOMF,NOM,MESSAG
      CHARACTER*80 NOMPB
C
      WRITE(6,*) 'SOUS PROGRAMME OPENF'
C
C VERIFICATION DU NOMBRE DE PARAMETRES DONNES
C LORS DU LANCEMENT DU PROGRAMME
C ------------------------------
      IF(IARGC().NE.1) THEN
        WRITE(6,*) 'IL MANQUE UN PARAMETRE A L''APPEL DU PROGRAMME'
        STOP
      ENDIF
C
C ON RECUPERE LE NOM DU PROBLEME = PREMIER ARGUMENT
C ------------------------------------------------- 
C      CALL GETARG(1,NOM)

	NOM = 'jobstpa_SIGMA_proto'
      IF(INDEX(NOM,".").NE.0) THEN
         NOMPB = NOM(1:INDEX(NOM,".")-1)
      ELSE
        NOMPB = NOM
      ENDIF
C
C FICHIER SPECTRE nompb.sigma
C ---------------------------
      FIN = 'sigma'  
      CALL RACINE(NOMPB,FIN,NOMF)
      OPEN(2,FILE=NOMF,FORM='FORMATTED',STATUS='UNKNOWN',ERR=900)
      WRITE(6,81) NOMF(IFDEB(NOMF):IFFIN(NOMF))
   81 FORMAT(' FICHIER SPECTRE             = ',A)
C
C FICHIER DOSSIER nompb.dossier
C -----------------------------
      FIN = 'dossier'
      CALL RACINE(NOMPB,FIN,NOMF)
      OPEN(3,FILE=NOMF,FORM='FORMATTED',STATUS='UNKNOWN',
     &     ACCESS='APPEND',ERR=900)
      WRITE(6,82) NOMF(IFDEB(NOMF):IFFIN(NOMF))
   82 FORMAT(' FICHIER DOSSIER             = ',A)
C
C FICHIER TRACE nompb.trace
C -------------------------
      FIN = 'trace'  
      CALL RACINE(NOMPB,FIN,NOMF)
      OPEN(4,FILE=NOMF,FORM='FORMATTED',STATUS='UNKNOWN',
     &     ACCESS='APPEND',ERR=900)
      WRITE(6,83) NOMF(IFDEB(NOMF):IFFIN(NOMF))
   83 FORMAT(' FICHIER TRACE               = ',A)
      WRITE(4,'(A)') 'MODE ECHO 0'
C
C FICHIER TRACE nompb.papniv
C --------------------------
      FIN = 'papniv'  
      CALL RACINE(NOMPB,FIN,NOMF)
      OPEN(7,FILE=NOMF,FORM='FORMATTED',STATUS='UNKNOWN',ERR=900)
      WRITE(6,84) NOMF(IFDEB(NOMF):IFFIN(NOMF))
   84 FORMAT(' FICHIER PAPNIV              = ',A)
C
C OUVERTURE DU FICHIER DATABASE MATERIAU
C --------------------------------------
      FIN = 'dbmat'
      CALL RACINE(NOMPB,FIN,NOMF)
      OPEN(8,FILE=NOMF,FORM='FORMATTED',STATUS='UNKNOWN',ERR=900)
      WRITE(6,85) NOMF(IFDEB(NOMF):IFFIN(NOMF))
   85 FORMAT(' FICHIER DATABASE MATERIAUX  = ',A)
C
C FICHIER TRACE nompb.traniv
C --------------------------
      FIN = 'traniv'  
      CALL RACINE(NOMPB,FIN,NOMF)
      OPEN(9,FILE=NOMF,FORM='FORMATTED',STATUS='UNKNOWN',ERR=900)
      WRITE(6,86) NOMF(IFDEB(NOMF):IFFIN(NOMF))
   86 FORMAT(' FICHIER TRANIV              = ',A)
C
C FICHIER TRACE nompb.picapic
C --------------------------
      FIN = 'picapic'  
      CALL RACINE(NOMPB,FIN,NOMF)
      OPEN(16,FILE=NOMF,FORM='FORMATTED',STATUS='UNKNOWN',ERR=900)
      WRITE(6,87) NOMF(IFDEB(NOMF):IFFIN(NOMF))
   87 FORMAT(' FICHIER PICAPIC             = ',A)
C
      RETURN
C
  901 FORMAT(1X,'ERREUR A L''OUVERTURE DU FICHIER ',A)
  900 WRITE(MESSAG,901) NOMF(IFDEB(NOMF):IFFIN(NOMF))
      CALL ERROR(NOMPB,MESSAG,19)
      END
C ----------------------------------
      SUBROUTINE OPENFI(NOMPB,OMEGA)
C ----------------------------------
C OUVERTURE DU FICHIER FILTRE
C ENTREES : NOMPB  NOM DU PROBLEME
C           OMEGA  VALEUR DE FILTRAGE 
C ***********************************
      CHARACTER*80 FIN,NOMF,NOM,MESSAG,CHOMEG
      CHARACTER*80 NOMPB
C
      WRITE(6,*) 'SOUS PROGRAMME OPENFI'
C
      WRITE(CHOMEG,'(F10.5)') OMEGA
      DO I=IFDEB(CHOMEG),IFFIN(CHOMEG)
        IF(INDEX(CHOMEG(I:I),'.').NE.0) CHOMEG(I:I) = 'p'
      ENDDO
      NOM = NOMPB(IFDEB(NOMPB):IFFIN(NOMPB))//'-'//
     &      CHOMEG(IFDEB(CHOMEG):IFFIN(CHOMEG))
C
C FICHIER FILTRE nompb.filtre
C ---------------------------
      FIN = 'sigma'  
      CALL RACINE(NOM,FIN,NOMF)
      OPEN(10,FILE=NOMF,FORM='FORMATTED',STATUS='UNKNOWN',ERR=900)
      WRITE(6,87) NOMF(IFDEB(NOMF):IFFIN(NOMF))
   87 FORMAT(' FICHIER FILTRE              = ',A)
C
      RETURN
C
  901 FORMAT(1X,'ERREUR A L''OUVERTURE DU FICHIER ',A)
  900 WRITE(MESSAG,901) NOMF(IFDEB(NOMF):IFFIN(NOMF))
      CALL ERROR(NOMPB,MESSAG,19)
      END
C -----------------------------------------------------
      SUBROUTINE PAPDES(SIG,NPIC,NBVOL,NUVOL,SDEB,SFIN)
C -----------------------------------------------------
C TRANSFORMATION D'UN SPECTRE VALIDE EN PICS-VALLEES
C ENTREES : SIG   CONTRAINTES
C           NPIC  NOMBRE DE CONTRAINTES
C           NBVOL NOMBRE DE VOLS
C           NUVOL NUMERO DU VOL 
C SORTIES : SIG   CONTRAINTES
C           SDEB  CONTRAINTE INITIALE DE LA SEQUENCE
C           SFIN  CONTRAINTE FINALE DU VOL
C ****************************************
      CHARACTER*80 NOMPB

	REAL SIG(*)
C
C      WRITE(6,*) 'SOUS PROGRAMME PAPDES'
C
C STOCKAGE DE LA CONTRAINTE INITIALE DE LA SEQUENCE
C -------------------------------------------------
      IF(NUVOL.EQ.1) SDEB = SIG(1)
C
C EGALITE ENTRE PREMIERE ET DERNIERE CONTRAINTE DU VOL
C ----------------------------------------------------
      NPIC = NPIC+1
      SIG(NPIC) = SIG(1)
C
C EGALITE ENTRE LA DERNIERE CONTRAINTE DU VOL i ET LA PREMIERE
C CONTRAINTE DU VOL i+1
C ---------------------
      IF(NUVOL.GE.2) THEN
        DO I=1,NPIC
          J        = NPIC-I+1
          SIG(J+1) = SIG(J)
        ENDDO
        SIG(1) = SFIN
        NPIC   = NPIC+1
      ENDIF
C
C RECHERCHE DE LA PREMIERE CONTRAINTE MAXIMALE
C --------------------------------------------
      K = 1
      DO I=1,NPIC-1
        IF(SIG(I).GT.SIG(I+1)) THEN
          K = I
          GOTO 1
        ENDIF
      ENDDO
    1 CONTINUE
      DO J=K,NPIC
        L      = J-K+1
        SIG(L) = SIG(K+L-1)
      ENDDO
      NPIC = NPIC-K+1
C
C VALIDATION DU SPECTRE
C ---------------------
      CALL VALIDE(NOMPB,SIG,NPIC)
C
C LA DERNIERE CONTRAINTE DOIT ETRE UNE VALLEE
C -------------------------------------------
      IF(MOD(NPIC,2).EQ.1) NPIC = NPIC-1
C
C EGALITE ENTRE POINT INITIAL ET FINAL DE LA SEQUENCE
C ---------------------------------------------------
      IF((NUVOL.EQ.NBVOL).AND.(SIG(NPIC).GT.SDEB)) THEN
        SIG(NPIC+1) = SIG(NPIC-1)
        SIG(NPIC+2) = SDEB
        NPIC        = NPIC+2
      ENDIF
C
C STOCKAGE DE LA CONTRAINTE FINALE DU VOL
C ---------------------------------------
      SFIN = SIG(NPIC)
C
      RETURN
      END
C -------------------------------------------------------------------
      SUBROUTINE PAPNIV(NOMPB,TITRE,SIG,SMAXS,NPIC,NBVOL,NUVOL,IECRI,
     &                  PMILLE,NIVMIN,NIVMAX,IRMIN,IRMAX,NECMIS,NVOLMI,
     &                  NVOLMA)
C -----------------------------
C ENTREES : NOMPB   NOM DU PROBLEME 
C           TITRE   TITRE DU PROBLEME
C           SIG     CONTRAINTES
C           SMAXS   CONTRAINTE MAX SUR LA SEQUENCE
C           NPIC    NOMBRE DE CONTRAINTES
C           NBVOL   NOMBRE DU VOL
C           NUVOL   NUMERO DU VOL
C           IECRI   FLAG D'ECRITURE
C SORTIES : PMILLE  FACTEUR DE CONVERSION NIVEAU-CONTRAINTE
C           NIVMIN  NIVEAU MIN SUR LA SEQUENCE
C           NIVMAX  NIVEAU MAX SUR LA SEQUENCE
C           IRMIN   RANG DU NIVEAU MIN
C           IRMAX   RANG DU NIVEAU MAX
C           NECMIS  PLUS PETIT ECART EN NIVEAU SUR LA SEQUENCE
C           NVOLMI  VOL CONTENANT LE NIVEAU MIN
C           NVOLMA  VOL CONTENANT LE NIVEAU MAX
C *********************************************
      PARAMETER(MAXSIG=800000,MAXCYC=MAXSIG/2)
C
      CHARACTER*80 NOMPB

	CHARACTER*(*) TITRE
      CHARACTER*100 MESSAG
      REAL SIG(*),SRF(MAXSIG)
      INTEGER NIV(MAXCYC)
C
      WRITE(6,*) 'SOUS PROGRAMME PAPNIV'
C     
      DATA NIVFIN/0/
      NECMIV = 1E6
C
C TRANSFORMATION DES CONTRAINTES EN NIVEAU
C ET RETOUR EN CONTRAINTES POUR LE CALCUL D'ALPHA
C -----------------------------------------------
      PMILLE = 1000./SMAXS
      DO I=1,NPIC,2
        NIV(I)   = 2*NINT(SIG(I)*PMILLE/2.)
        NIV(I+1) = 2*NINT(SIG(I+1)*PMILLE/2.)
        SRF(I)   = FLOAT(NIV(I))/PMILLE
        SRF(I+1) = FLOAT(NIV(I+1))/PMILLE
      ENDDO
C
C SUPPRESSION DES NIVEAUX EGAUX
C -----------------------------
      DO I=2,NPIC,2
        IF(NIV(I).EQ.NIV(I-1)) THEN
          DO J=I+1,NPIC
            NIV(J-2) = NIV(J)
          ENDDO
          NPIC = NPIC-2
        ENDIF
      ENDDO
C
C STOCKAGE DU PREMIER NIVEAU DE LA SEQUENCE
C -----------------------------------------
      IF(NUVOL.EQ.1) THEN
        REWIND(9)
        NIVDEB = NIV(1)
      ENDIF
C
C CALCUL DU PLUS PETIT ECART PIC-VALLEE EN NIVEAU SUR LE VOL
C CONVENTIONS SI LE MIN EST ENTRE LE VOL i ET LE VOL i+1,IL EST AFFECTE AU VOL i+1
C             SI LE MIN EST ENTRE LE VOL nuvol ET LE VOL 1, IL EST AFFECTE AU VOL nuvol
C -------------------------------------------------------------------------------------
      DO I=2,NPIC
        NECMIV = MIN(NECMIV,ABS(NIV(I)-NIV(I-1)))
      ENDDO
      IF(NUVOL.NE.1)     NECMIS = MIN(NECMIS,(NIV(1)-NIVFIN))
      IF(NUVOL.EQ.NBVOL) NECMIS = MIN(NECMIS,NIVDEB-NIV(NPIC)) 
C
C CALCUL DES NIVEAUX MIN ET MAX ET REPASSAGE EN CONTRAINTE
C --------------------------------------------------------
      CALL MINMAX(SIG,NPIC,SMAXV,SMINV,IMAXV,IMINV)
      NMAX = 2*NINT(SMAXV*PMILLE/2.)
      NMIN = 2*NINT(SMINV*PMILLE/2.)
C
C CALCUL D'ALPHA
C --------------
      CALL CALPHA(SRF,SMAXS,NPIC,NBVOL,NUVOL,0,ALPHA,ALPHAS)
C
C STOCKAGE DU DERNIER NIVEAU DU VOL
C ---------------------------------
      NIVFIN = NIV(NPIC)
C
C CALCUL DES RANGS MIN ET MAX (PREMIERE APPARITION)
C -------------------------------------------------
      IMIN   = NPIC
      IMAX   = NPIC
      KCOMPT = 0
      DO I=1,NPIC,2
        KCOMPT = KCOMPT+1
        IF((NIV(I).EQ.NMAX).AND.(KCOMPT.LE.IMAX))   IMAX = KCOMPT
        IF((NIV(I+1).EQ.NMIN).AND.(KCOMPT.LE.IMIN)) IMIN = KCOMPT
      ENDDO
C
      IF(IECRI.EQ.1) THEN
        WRITE(3,210) 
        WRITE(3,220) 'NIVEAU MAX',NMAX,'CYCLE',IMAX
        WRITE(3,220) 'NIVEAU MIN',NMIN,'CYCLE',IMIN
        WRITE(3,230) 'PLUS PETIT ECART PIC-VALLEE EN NIVEAU',NECMIV,' ',
     &               ' '
        WRITE(3,240) 'CONTRAINTE MAX',FLOAT(NMAX)/PMILLE,' ',' '
        WRITE(3,240) 'CONTRAINTE MIN',FLOAT(NMIN)/PMILLE,' ',' '
        WRITE(3,240) 'PLUS PETIT ECART PIC-VALLEE EN CONTRAINTE',
     &               FLOAT(NECMIV)/PMILLE,' ',' '
        WRITE(3,210) 
      ENDIF
  210 FORMAT(1X,':-------------------------------------------:----------
     &------:--------:---------:')
  220 FORMAT(1X,': ',A41,' : ',I14,' : ',A6,' : ',I7,' :')
  230 FORMAT(1X,': ',A41,' : ',I14,' : ',A6,' : ',A7,' :')
  240 FORMAT(1X,': ',A41,' : ',F14.5,' : ',A6,' : ',A7,' :')
  156 FORMAT(5I10,E10.4,2I10)
  157 FORMAT(6I10,E10.4,2I10)
  158 FORMAT(SP,16I6)
      WRITE(9,157) NPIC,NMAX,IMAX,NMIN,IMIN,NECMIV,ALPHA,NUVOL
      WRITE(9,158) (NIV(I),I=1,NPIC)
C
C CALCUL DU PLUS PETIT ECART PIC-VALLEE EN NIVEAU SUR LA SEQUENCE ET VOL CORRESPONDANT
C CALCUL DU MAX DES NIVEAUX SUR LA SEQUENCE (1000) ET DU MIN
C ----------------------------------------------------------
      NECMIS = MIN(NECMIS,NECMIV)
      IF(NMAX.GE.NIVMAX) THEN
        NIVMAX = NMAX
        NVOLMA = NUVOL
      ENDIF  
      IF(NMIN.LE.NIVMIN) THEN
        NIVMIN = NMIN
        NVOLMI = NUVOL
      ENDIF 
C 
C CONTROLE DE LA MONOTONIE
C ------------------------
      IF(NUVOL.EQ.1) NIV1 = NIV(1)
      IF(NUVOL.NE.1) THEN
        IF(NIV(1).LE.NIVFIN) THEN
  120     FORMAT(1X,'VOL N ',I4,' NIVEAU 1 (PIC) <= DERNIER NIVEAU VOL P
     &RECEDENT (VALLEE)')
          WRITE(MESSAG,120) NUVOL
          CALL ERROR(NOMPB,MESSAG,19)
        ENDIF
      ENDIF
C
      IF(NUVOL.EQ.NBVOL) THEN
        IF(NIV1.LE.NIVFIN) THEN
  130     FORMAT(1X,'PREMIER NIVEAU SEQUENCE <= DERNIER NIVEAU SEQUENCE'
     &)
          WRITE(MESSAG,130) 
          CALL ERROR(NOMPB,MESSAG,19)
        ENDIF
      ENDIF
C
      DO I=2,NPIC-1,2
        IF(NIV(I).GE.NIV(I-1)) THEN  
  100     FORMAT(1X,'VOL N ',I4,' NIVEAU ',I6,' (VALLEE) >= NIVEAU PRECE
     &DENT',I4,' (PIC)')
          WRITE(MESSAG,100) NUVOL,I,I-1
          CALL ERROR(NOMPB,MESSAG,19)
        ENDIF
        IF(NIV(I+1).LE.NIV(I)) THEN  
  110     FORMAT(1X,'VOL N ',I4,' NIVEAU ',I6,' (PIC) <= NIVEAU PRECEDEN
     &T',I4,' (VALLEE)')
          WRITE(MESSAG,110) NUVOL,I+1,I
          CALL ERROR(NOMPB,MESSAG,19)
        ENDIF
      ENDDO
C
C ECRITURE SUR LE FICHIER prefixe.papniv
C --------------------------------------
      IF((NUVOL.EQ.NBVOL).AND.(IECRI.EQ.0)) THEN
        KCOMPT = 0
        IRMIN  = NBVOL*MAXSIG/2
        IRMAX  = NBVOL*MAXSIG/2
        REWIND(9)
        REWIND(7)
        WRITE(7,'(I10)') 8
        WRITE(7,'(A)') TITRE
        WRITE(7,156) NIVMAX,IRMAX,NIVMIN,IRMIN,NECMIS,ALPHAS,NBVOL,
     &               KCOMPT
        DO I=1,NBVOL
          READ(9,157) NVAL,NMAX,IMAX,NMIN,IMIN,NECMIV,ALPHA,NUVOL
          READ(9,158) (NIV(J),J=1,NVAL)
          WRITE(7,157) NVAL,NMAX,IMAX,NMIN,IMIN,NECMIV,ALPHA,NUVOL
          WRITE(7,158) (NIV(J),J=1,NVAL)
C
C CALCUL DES RANGS MIN ET MAX SUR LA SEQUENCE (PREMIERE APPARITION)
C -----------------------------------------------------------------
          DO K=1,NVAL,2
            KCOMPT = KCOMPT+1
            IF((NIV(K).EQ.NIVMAX).AND.(KCOMPT.LE.IRMAX))   
     &      IRMAX = KCOMPT
            IF((NIV(K+1).EQ.NIVMIN).AND.(KCOMPT.LE.IRMIN)) 
     &      IRMIN = KCOMPT
          ENDDO
        ENDDO
C
        WRITE(7,'(A15)') 'FIN DE SECTION'
C
  119 FORMAT(1X,A12,I6,A8,I7,A6,I5)
        WRITE(6,119) 'NIVEAU MAX =',NIVMAX,' CYCLE =',IRMAX,' VOL =',NVO
     &LMA
        WRITE(6,119) 'NIVEAU MIN =',NIVMIN,' CYCLE =',IRMIN,' VOL =',NVO
     &LMI
        WRITE(6,'(1X,A39,I6)') 'PLUS PETIT ECART PIC-VALLEE EN NIVEAU ='
     &,NECMIS
        WRITE(6,'(1X,A16,F10.5)') 'CONTRAINTE MAX =',FLOAT(NIVMAX)/PMILL
     &E
        WRITE(6,'(1X,A16,F10.5)') 'CONTRAINTE MIN =',FLOAT(NIVMIN)/PMILL
     &E
        WRITE(6,'(1X,A43,F10.5)') 'PLUS PETIT ECART PIC-VALLEE EN CONTRA
     &INTE =',FLOAT(NECMIS)/PMILLE
      ENDIF
C
      RETURN
      END
C ------------------------------------
      SUBROUTINE POSITI(NOMPB,IU,ILIG)
C ------------------------------------
C POSITIONNEMENT DANS LE FICHIER prefixe.sigma
C ENTREES : NOMPB  NOM DU PROBLEME
C           IU     UNITE DU FICHIER SIGMA
C SORTIES : ILIG   NUMERO DE LA LIGNE
C ***********************************
      CHARACTER*80 NOMPB
      CHARACTER*110 CARTE
      CHARACTER*80  MESSAG
C
      WRITE(6,*) 'SOUS PROGRAMME POSITI'
C
      REWIND(IU)
      ILIG = 1
C
C POSITIONNEMENT SUR LA PREMIERE INSTRUCTION NON ABRE
C ---------------------------------------------------
    1 READ(IU,'(A)',ERR=900) CARTE
      IF(INDEX(CARTE,'ABRE').EQ.0) GOTO 2
      ILIG = ILIG+1
      GOTO 1
    2 CONTINUE
      BACKSPACE(IU)
      ILIG = ILIG-1
      RETURN
C
  901 FORMAT(1X,'ERREUR A LA LECTURE DU FICHIER SPECTRE LIGNE= ',I6)
  900 WRITE(MESSAG,901) ILIG
      CALL ERROR(NOMPB,MESSAG,19)
      END
C ----------------------------------------------------------------------
      SUBROUTINE PREFAS(NOMPB,SIG,HMA,HMI,HO,VMICL,SMAXS,SMINS,A,B,C,XN,
     &                  R02,RM,IPAS,NPIC,ICOMP,IECRI,EF,EFCL,N)
C -------------------------------------------------------------
C ENTREES : NOMPB           NOM DU PROBLEME 
C           SIG             CONTRAINTES
C           HMA,HMI,HO      VALEURS D'HISTOIRE
C           VMICL           VALEUR MINI POUR LE CUMUL LINEAIRE
C           SMAXS,SMINS     CONTRAINTE MAXIMALE,MINIMALE SUR LA SEQUENCE
C           A,B,C,XN,R02,RM PARAMETRES D'ELBER
C           IPAS            =0 PREMIER PASSAGE
C           NPIC            NOMBRE DE CONTRAINTES
C           ICOMP           =1 COMPRESSION =2 PAS DE COMPRESSION PREFFAS
C           IECRI           FLAG D'ECRITURE
C SORTIES : EF              EFFICACITE DE LA SEQUENCE
C           EFCL            EFFICACITE EN CUMUL LINEAIRE
C           VMICL           VALEUR MINI POUR LE CUMUL LINEAIRE
C           N               COMPTEUR DE CYCLES D'HISTOIRE
C
C SIGNIFICATION DES DONNEES
C EF         : EFFICACITE DE LA SEQUENCE
C EFCL       : EFFICACITE EN CUMUL LINEAIRE
C HMA,HMI,HO : PARAMETRES D' HISTOIRE
C VMICL      : VALEUR MINI POUR LE CUMUL LINEAIRE
C N          : COMPTEUR DE CYCLES D' HISTOIRE
C I          : COMPTEUR DE CYCLES
C JA         : COMPTEUR DES CYCLES QUI N'AVANCENT PAS
C JB         : COMPTEUR DES CYCLES QUI AVANCENT SANS CUMUL CONSERVATIF
C JC         : COMPTEUR DES CYCLES QUI AVANCENT AVEC CUMUL CONSERVATIF
C ********************************************************************
      PARAMETER(NHMAX=50)
C
      CHARACTER*80 NOMPB
      CHARACTER*80 MESSAG
      REAL SIG(*),HMA(*),HMI(*),HO(*),EFi,EFCLi
C
C      WRITE(6,*) 'SOUS PROGRAMME PREFAS (vol,npic)',IPAS,NPIC
C
   10 FORMAT(1X,':--------------------------------:----------------:----
     &----:-------:')
   20 FORMAT(1X,': ',A30,' : ',E14.7,' : ',A6,' : ',A5,' :') 
C
      IF (IPAS.LE.1) THEN
        JA   = 0
        JB   = 0
        JC   = 0
        EF   = 0.
        EFCL = 0.
      ENDIF  
      EFi=EF
      EFCLi=EFCL
C
C      if (IPAS.GT.0) THEN
C       write(16,'(/,A,/,A,I4)') ' ************************',
C     &                         ' PIC A PIC - VOL : ',IPAS
c       do i=1,npic
c         write(16,'(I6,1X,E13.6)') I,sig(i)
c       enddo
C      endif
      DO I=1,NPIC,2
        VMAI = SIG(I)
        VMII = SIG(I+1)
        NDEB=0
C
C SUPPRESSION DES ALTERNANCES ENTIEREMENT NEGATIVES
C -------------------------------------------------
        IF(VMAI.GT.0.) THEN  
C
          IF((ICOMP.EQ.2).AND.(VMII.LT.0.)) VMII = 0.
C      if (IPAS.GT.0) THEN
C         write(16,'(I6,1X,E13.6)') I,VMAI
C         write(16,'(I6,1X,E13.6)') I+1,VMII
C      endif
C
C CALCUL DE EFCL EFFICACITE EN CUMUL LINEAIRE
C -------------------------------------------
          IF(VMICL.LT.0.) THEN
            G     = (1.-A)/A
            UCL   = A*(1.+2.*G*ABS(VMICL)/(R02+RM))
            VOCL  = VMAI*(1.-UCL)
          ELSE  
            RCL   = VMICL/VMAI
            UCL   = A+B*RCL+C*RCL**2
            VOCL  = VMAI-UCL*(VMAI-VMICL)
          ENDIF
          EFCL  = EFCL+(VMAI-VOCL)**XN
C
C CALCUL DU POINT D'OUVERTURE AU CYCLE i
C --------------------------------------
          IF(VMII.LT.0.) THEN
            G     = (1.-A)/A
            VOI   = VMAI*(1.-A*(1.+2.*G*ABS(VMII)/(R02+RM)))
            VMICL = 0.
          ELSE
            RI    = VMII/VMAI
            UI    = A+B*RI+C*RI**2
            VOI   = VMAI-UI*(VMAI-VMII)
            VMICL = VMII
          ENDIF
C
C TEST DE CUMUL CONSERVATIF
C LA VALEUR MAX VMA(i) EST ELLE SUPERIEURE A L'UNE DES VALEURS D'HISTOIRE
C QUI PRECEDENT J=1,N NOMBRE DE VALEURS D'HISTOIRE
C VMA(i)>HMA(j)
C -------------
          DO J=1,N
            IF(VMAI.GT.HMA(J)) GOTO 1
          ENDDO
C
C CALCUL DE L'AVANCEE SANS CUMUL CONSERVATIF
C COMPARAISON ENTRE LA VALEUR MAX AU CYCLE i VMA(i) ET LA DERNIERE
C VALEUR D'OUVERTURE D'HISTOIRE HO(n) - VMA(i)>HO(n) 
C SINON PAS D'AVANCEE
C -------------------
          IF(VMAI.GT.HO(N)) THEN
C
C AVANCEE SANS CUMUL CONSERVATIF - CALCUL DE L'EFFICACITE
C -------------------------------------------------------
            JB = JB+1
            EF = EF+(VMAI-HO(N))**XN
C            IF (IECRI.EQ.2) THEN
C            write(6,'(2A,50X,A,I8,2F10.5)') '--> EF SANS CUMUL ',
C     &       'CONSERVATIF: EF=EF + (VMAI-HO(N))**XN',
C     &       ' I,Pic/Vallee = ',I,SIG(I),SIG(I+1)
C            write(6,'(23X,A,I5,19X,2F10.5)')'  HO   n=',N,VMAI,HO(N)
C            endif
            NDEB=1
          ELSE
            JA = JA+1
          ENDIF
C
C TEST DE SOUS CHARGE - COMPARAISON ENTRE LA VALEUR MIN AU CYCLE i VMI(i)
C ET LA DERNIERE VALEUR MIN D'HISTOIRE HMI(n)
C -------------------------------------------
          IF(VMII.LT.HMI(N)) THEN
C
C EFFET DE SOUS CHARGE - k VALEUR D'HISTOIRE TELLE QUE VMI(i)<HMI(k)
C ------------------------------------------------------------------
            K = 1
            DO WHILE(VMII.GE.HMI(K))
              K = K+1
            ENDDO
C
C CALCUL DU POINT D'OUVERTURE HO(k) DU A HMA(k) ET VMI(i)
C -------------------------------------------------------
            IF(VMII.LT.0.) THEN
              G      = (1.-A)/A
              HO(K)  = HMA(K)*(1.-A*(1.+2*G*ABS(VMII)/(R02+RM)))
            ELSE
              RI     = VMII/HMA(K)
              UI     = A+B*RI+C*RI**2
              HO(K)  = HMA(K)-UI*(HMA(K)-VMII)
            ENDIF
            HMI(K) = VMII
C
C CETTE VALEUR D'HISTOIRE HO(k) EST ELLE SUPERIEURE A CELLE QUI 
C LA PRECEDE
C ----------
            IF(HO(K).GT.HO(K-1)) THEN
C
C DERNIERE VALEUR D'HISTOIRE A CONSIDERER k
C -----------------------------------------
              N = K
            ELSE
C
C DERNIERE VALEUR D'HISTOIRE A CONSIDERER k-1
C -------------------------------------------
              N = K-1
            ENDIF
C
          ELSE
C
C TEST DE NIVEAU D'HISTOIRE SUPPLEMENTAIRE
C ----------------------------------------
            IF(VOI.GT.HO(N)) THEN
C
C ENREGISTREMENT DU NIVEAU n=n+1
C ------------------------------
              N = N+1
              IF(N.GT.NHMAX) THEN
  100           FORMAT(1X,'ZONES PREFFAS SATUREES')
                CALL ERROR(NOMPB,MESSAG,19)
              ENDIF
              HMA(N) = VMAI
              HMI(N) = VMII
              HO(N)  = VOI
            ENDIF
          ENDIF  
          GOTO 2
C         
    1     CONTINUE
C
C CALCUL DE L'AVANCEE AVEC CUMUL CONSERVATIF - CALCUL DE L'EFFICACITE
C -------------------------------------------------------------------
          JC = JC+1
C          IF (IECRI.EQ.2) THEN
C          write(6,'(3A,I9,2F10.5)') '--> EF AVEC CUMUL CONSERVATIF: EF',
C     &     '=EF + (VMAI-HO(J-1))**XN +(HMA(L)-HO(L))**XN - (HMA(L)-',
C     &     'HO(L-1))**XN      I,Pic/Vallee = ',I,SIG(I),SIG(I+1)
C          write(6,'(23X,A,I5,2F10.5)')'  HO j-1=',J-1,VMAI,HO(J-1)
C          endif
          CC = (VMAI-HO(J-1))**XN
          DO L=J,N
            CC = CC+(HMA(L)-HO(L))**XN-(HMA(L)-HO(L-1))**XN
C            IF (IECRI.EQ.2) THEN
C            write(6,'(23X,A,I5,14X,2(5X,2F10.5))') 
C     &         '  HO   l=',L,HMA(L),HO(L),HMA(L),HO(L-1)
C            endif
          ENDDO
          NDEB=1
          EF = EF+CC
C
C TEST DE SOUS-CHARGE VMI(i)<HMI(j-1)
C -----------------------------------
          IF(VMII.LT.HMI(J-1)) THEN
C
C EFFET DE SOUS CHARGE - k VALEUR D'HISTOIRE TELLE QUE VMI(i)<HMI(k)
C ------------------------------------------------------------------
            K = 1
            DO WHILE(VMII.GE.HMI(K))
              K = K+1
            ENDDO
C
C CALCUL DU POINT D'OUVERTURE HO(k) DU A HMA(k) ET VMI(i)
C -------------------------------------------------------
            IF(VMII.LT.0.) THEN
              G     = (1.-A)/A
              HO(K) = HMA(K)*(1.-A*(1.+2*G*ABS(VMII)/(R02+RM)))
            ELSE
              RI     = VMII/HMA(K)
              UI     = A+B*RI+C*RI**2
              HO(K)  = HMA(K)-UI*(HMA(K)-VMII)
            ENDIF  
            HMI(K) = VMII
C
C CETTE VALEUR D'HISTOIRE HO(k) EST ELLE SUPERIEURE A CELLE QUI 
C LA PRECEDE
C ----------
            IF(HO(K).GT.HO(K-1)) THEN
C
C DERNIERE VALEUR D'HISTOIRE A CONSIDERER k
C -----------------------------------------
              N = K
            ELSE
C
C DERNIERE VALEUR D'HISTOIRE A CONSIDERER k-1
C -------------------------------------------
              N = K-1
            ENDIF
          ELSE  
C
C TEST DE NIVEAU D'HISTOIRE SUPPLEMENTAIRE
C L'OUVERTURE DU CYCLE EST ELLE SUPERIEURE A CELLE DE LA VALEUR D'HISTOIRE j-1
C VO(i)>HO(j-1)
C -------------
            IF(VOI.GT.HO(J-1)) THEN
C
C ENREGISTREMENT DE NIVEAU - VALEUR D'HISTOIRE j ET STOCKAGE
C ----------------------------------------------------------
              N      = J
              HMA(N) = VMAI
              HMI(N) = VMII
              HO(N)  = VOI
            ELSE 
C
C ENREGISTREMENT DE NIVEAU - VALEUR D'HISTOIRE j-1
C ------------------------------------------------
              N = J-1
            ENDIF
          ENDIF
    2     CONTINUE

          IF ((IECRI.EQ.2).AND.(NDEB.NE.0)) THEN
C           WRITE(6,'(A,50F10.5)') 'HMA  = ', (HMA(K),K=1,N)
C           WRITE(6,'(A,50F10.5)') 'HMI  = ', (HMI(K),K=1,N)
C           WRITE(6,'(A,50F10.5)') 'HO   = ', (HO(K),K=1,N)
C           WRITE(6,*) 'EF   = ', EF
C           WRITE(6,*) 'EFCL = ',EFCL
           NDEB=N
          ENDIF
        ENDIF

      ENDDO
C
C      IF (IECRI.EQ.2) THEN
C      WRITE(6,'(/,A)') ' SORTIE DE PREFAS'
C       WRITE(6,*) 'N    = ',N
C       WRITE(6,'(A,50F10.5)') 'HMA  = ', (HMA(K),K=1,N)
C       WRITE(6,'(A,50F10.5)') 'HMI  = ', (HMI(K),K=1,N)
C       WRITE(6,'(A,50F10.5)') 'HO   = ', (HO(K),K=1,N)
C       WRITE(6,'(A,E16.8,A,I5)')
C     &   'EFFICACITE (PREFFAS)(MPa**n)',EF-EFi,' Vol N°',IPAS
C       WRITE(6,'(A,E16.8,A,I5,/)')
C     &   'EFFICACITE LINEAIRE (MPa**n)',EFCL-EFCLi,' Vol N°',IPAS
C      ENDIF
C
C      IF(IECRI.EQ.1) THEN
C        WRITE(3,20) 'EFFICACITE (MPa**n)',EF,' ',' '
C        WRITE(3,20) 'EFFICACITE LINEAIRE (MPa**n)',EFCL,' ',' '
C        WRITE(3,10)
C      ENDIF
C
C CALCUL DES VALEURS DE SORTIE
C EQUI : NOMBRE DE CYCLES ( SOUS CHARGEMENT D' AMPLITUDE
C        CONSTANTE ENTRE PMAREF ET PMIREF ) EQUIVALENTS A
C        UNE SEQUENCE
C 
      VMAREF = SMAXS
      VMIREF = SMINS
      IF((ICOMP.EQ.2).AND.(SMINS.LE.0)) VMIREF = 0.1*VMAREF
      IF(VMAREF.GT.0.) THEN
        IF(VMIREF.LT.0.) THEN
          G     = (1.-A)/A
          U     = A*(1.+2.*G*ABS(VMIREF)/(R02+RM))
          DVEF  = U*VMAREF
        ELSE
          R    = VMIREF/VMAREF
          U    = A+B*R+C*R**2
          DVEF = U*(VMAREF-VMIREF)
        ENDIF  
        EQUI = EF/(DVEF**XN)
C        IF ((IECRI.EQ.1).OR.(IECRI.EQ.2)) THEN
C          WRITE(6,110) VMAREF,VMIREF,EQUI,EF,EFCL
C        ENDIF
      ENDIF
C
  110 FORMAT (1X,'CONTRAINTE MAX. DE REF.  ',12X,F14.2,
     &       ' MPa',/,1X,'CONTRAINTE MIN. DE REF.  ',12X,F14.2,
     &       ' MPa',/,1X,'NB. DE CYCLES EQUIVALENTS',20X,F14.2,/,/,1X,
     &       'EFFICACITE CUMULEE (PREFFAS)',17X,F14.2,' MPa**N',/,1X,
     &       'EFFICACITE  (CUMUL LINEAIRE SANS RAIN-FLOW)',2X,F14.2,
     &       ' MPa**N')
C
      RETURN
      END
C -------------------------------------
      SUBROUTINE RESTRU(SIG,NPIC,IMINV)
C -------------------------------------
C RESTRUCTURATION DU VOL
C ENTREES : SIG   CONTRAINTES DU VOL
C           NPIC  NOMBRE DE CONTRAINTES
C           IMINV RANG DE LA CONTRAINTE MINIMALE
C **********************************************
      PARAMETER(MAXSIG=800000)
C
      REAL SIG(*),XTRAV(MAXSIG)
C
      WRITE(6,*) 'SOUS PROGRAMME RESTRU'
C
C LA PREMIERE CONTRAINTE EST LA CONTRAINTE MINIMALE
C TOUTES LES CONTRAINTES PRECEDENTES SONT PERMUTEES A LA FIN
C ----------------------------------------------------------
      DO I=1,IMINV-1
        XTRAV(I) = SIG(I)
      ENDDO
      DO I=1,NPIC-IMINV+1
        SIG(I) = SIG(IMINV+I-1)
      ENDDO
      DO I=1,IMINV-1
        SIG(NPIC-IMINV+1+I) = XTRAV(I)
      ENDDO
C
C LA DERNIERE CONTRAINTE EST LA CONTRAINTE MINIMALE
C -------------------------------------------------
      NPIC = NPIC+1
      SIG(NPIC) = SIG(1)
C
C     K = -10
C     DO I=1,NPIC/10
C       K = K+10
C       WRITE(6,'(10(F10.5,1X))') (SIG(J+K),J=1,10)
C     ENDDO
C     WRITE(6,'(10(F10.5,1X))') (SIG(NPIC/10*10+L),L=1,NPIC-NPIC/10*10)
C
      RETURN
      END
C -----------------------------------------------------
      SUBROUTINE RFLOW(SIG,NPIC,NUVOL,IECRI,SRF,NCYCLE)
C -----------------------------------------------------
C RAIN-FLOW : UTILISATION DES TROIS REGLES D'ECOULEMENT
C ENTREES : SIG    CONTRAINTES DU VOL
C           NPIC   NOMBRE DE CONTRAINTES
C           NUVOL  NUMERO DU VOL
C           IECRI  FLAG D'ECRITURE
C SORTIES : SRF    CYCLES
C           NCYCLE NOMBRE DE CYCLES
C *********************************
      PARAMETER(MAXSIG=800000)
C
      REAL SIG(*),SRF(2,*)
      INTEGER IMOUIL(MAXSIG)
C
      WRITE(6,*) 'SOUS PROGRAMME RFLOW' 
C
C INITIALISATION
C --------------
      DO I=1,NPIC
        IMOUIL(I) = 0
      ENDDO
      K = 1
C
C I IMPAIR = VALLEE
C J PAIR   = PIC
C SIG(I)    PIC DE DEPART
C SIG(JMAX) PIC D'ARRIVEE
C ------------------------
      DO I=1,NPIC-2,2
        J = I+1
C
C NON RENCONTRE D'UN ECOULEMENT ANTERIEUR
C ---------------------------------------
        IF(IMOUIL(J).EQ.0) THEN
          IMOUIL(J) = K
          JMAX      = J
C
    1     IF(J.LE.NPIC-2) THEN
C
C LA CONTRAINTE EST SUPERIEURE A LA CONTRAINTE INITIALE
C -----------------------------------------------------
            IF(SIG(J+1).GE.SIG(I)) THEN
C
C CAS D'UNE SURFACE NON MOUILLEE
C ------------------------------
              IF(IMOUIL(J+2).EQ.0) THEN
C 
C CAS OU L'ECOULEMENT CONTINUE
C ----------------------------
                IF(SIG(J+2).GE.SIG(JMAX)) THEN
                  IMOUIL(J+2) = K
                  JMAX        = J+2
                ENDIF
C 
C CAS OU L'ECOULEMENT S'ARRETE
C ----------------------------
                J = J+2
                GOTO 1
C
C SURFACE DEJA MOUILLEE - CALCUL DU PIC DE DEPART
C -----------------------------------------------
              ELSE
                L    = IMOUIL(J+2)
                JSTO = J+2
                DO WHILE(IMOUIL(J).NE.L)
                  J = J-2
                ENDDO
                JMAX = J
C               
C MISE A JOUR DU TABLEAU IMOUIL
C -----------------------------
                DO I1=I+1,JSTO,2
                  IF(IMOUIL(I1).EQ.K) IMOUIL(I1) = L
                ENDDO
              ENDIF
C
            ELSE
C
C LA CONTRAINTE EST INFERIEURE A LA CONTRAINTE INITIALE
C -----------------------------------------------------
              DO WHILE(IMOUIL(J).NE.K) 
                J = J-2
              ENDDO
              JMAX = J 
            ENDIF
          ENDIF
        ELSE
C
C RENCONTRE D'UN ECOULEMENT ANTERIEUR
C -----------------------------------
          L = IMOUIL(J)  
          J = J-2
          DO WHILE(IMOUIL(J).NE.L) 
            J = J-2
          ENDDO
          JMAX = J 
        ENDIF
C
        SRF(1,K) = SIG(JMAX)
        SRF(2,K) = SIG(I)
        K        = K+1
      ENDDO
      NCYCLE = K-1
C
C CONTRAINTE MOYENNE PONDEREE
C ---------------------------
   20 FORMAT(1X,': ',A30,' : ',F14.5,' : ',A6,' : ',A5,' :') 
      XK     = 2.
      CMPOND = 0.
      DO I=1,NCYCLE
        CMPOND = CMPOND+(SRF(1,I)-SRF(2,I))**XK
      ENDDO
      IF(NCYCLE.NE.0) CMPOND = (CMPOND/FLOAT(NCYCLE))**(1./XK)
      IF(IECRI.EQ.1) THEN
        WRITE(3,20) 'CONTRAINTE MOYENNE PONDEREE',CMPOND,' ',' '
      ENDIF
C
      RETURN
      END
C ------------------------------------------
      SUBROUTINE STOCK1(NOMPB,SIG,NPIC,SIG1)
C ------------------------------------------
C RECHERCHE DE LA PREMIERE CONTRAINTE DE LA SEQUENCE
C ENTREES : NOMPB  NOM DU PROBLEME
C           SIG    CONTRAINTES
C           NPIC   NOMBRE DE CONTRAINTES
C SORTIES : SIG1   PREMIERE CONTRAINTE
C ************************************
      CHARACTER*80 NOMPB
      REAL SIG(*)
C
      WRITE(6,*) 'SOUS PROGRAMME STOCK1'
      DATA ISTO1/0/
C
      IF(ISTO1.EQ.0) SIG1 = -9999.
      IF((NPIC.NE.0).AND.(ISTO1.EQ.0)) THEN
        ISTO1 = 1
        SIG1  = SIG(1)
      ENDIF
C
      RETURN
      END
C ------------------------------------------
      SUBROUTINE STOCKN(NOMPB,SIG,NPIC,SIGN)
C ------------------------------------------
C RECHERCHE DE LA DERNIERE CONTRAINTE DU VOL
C ENTREES : NOMPB  NOM DU PROBLEME
C           SIG    CONTRAINTES
C           NPIC   NOMBRE DE CONTRAINTES
C SORTIES : SIGN   DERNIERE CONTRAINTE
C ************************************
      CHARACTER*80 NOMPB
      REAL SIG(*)
C
      WRITE(6,*) 'SOUS PROGRAMME STOCKN'
      DATA ISTON/0/
C
      IF(ISTON.EQ.0) SIGN = -9999.
      IF(NPIC.NE.0)  SIGN  = SIG(NPIC)
C
      RETURN
      END
C --------------------------------------------------------------
      SUBROUTINE TRANSI1(NOMPB,SIG,SIGN,OMEGA,SIGMAD,NUVOL,NPIC)
C --------------------------------------------------------------
C TRANSITION ENTRE VOLS
C ENTREES : NOMPB  NOM DU PROBLEME
C           SIG    CONTRAINTES
C           SIGN   DERNIERE CONTRAINTE DU VOL i-1
C           OMEGA  VALEUR DU FILTRAGE
C           SIGMAD LIMITE D'ENDURANCE
C           NUVOL  NUMERO DU VOL
C           NPIC   NOMBRE DE CONTRAINTES
C SORTIES : SIG    CONTRAINTES 
C           NPIC   NOMBRE DE CONTRAINTES
C **************************************
      CHARACTER*80 NOMPB
      REAL SIG(*)
C
      WRITE(6,*) 'SOUS PROGRAMME TRANSI1'
C
C RECHERCHE DE LA PREMIERE ALTERNANCE SIGNIFICATIVE DE TRANSITION
C ---------------------------------------------------------------
      IF(SIGN.NE.-9999.) THEN
        S2 = SIGN
        DO I=1,NPIC
          S1 = SIG(I)
          SEUIL = FSEUIL(S1,S2,OMEGA,SIGMAD)
          IF(ABS(S1-S2).GE.SEUIL) GOTO 1
        ENDDO
        WRITE(6,*) 'VOL NUMERO =',NUVOL,' VIDE'
        NPIC = 0
        GOTO 2
C
    1   CONTINUE
CModif D. Grimald : le 09/01/20001
C
C CONTROLE QUE LA PREMIERE CONTRAINTE EST DE TYPE PIC
C ---------------------------------------------------
        DO J=I,NPIC-1
          IF (SIG(J).LE.SIG(J+1)) THEN
            I=J+1  
          ELSE
            GOTO 3
          ENDIF
        ENDDO 
CModif D. Grimald : le 09/01/20001
C
 3      CONTINUE
        DO J=I,NPIC
          SIG(J-I+1) = SIG(J)
        ENDDO
        NPIC = NPIC-I+1
      ENDIF  
C
C CONTROLE QUE LA DERNIERE CONTRAINTE EST DE TYPE VALLEE
C ------------------------------------------------------
 2    IF (NPIC.GE.2) THEN
        DO J=NPIC,2,-1
          IF (SIG(J).GE.SIG(J-1)) THEN
             NPIC=NPIC-1
          ELSE
             RETURN
          ENDIF
        ENDDO 
      ENDIF
      RETURN
      END
C --------------------------------------------------------------
      SUBROUTINE TRANSI2(NOMPB,SIG,SIGN,OMEGA,SIGMAD,NUVOL,NPIC)
C --------------------------------------------------------------
C TRANSITION ENTRE VOLS (Impose que le premier Vol commence par un Pic)
C                        et donc le dernier vol se terminera par une vallee)
C                       (Impose que le premier Vol se termine par une vallee)
C                        et donc deuxieme vol commencera par un pic)
C Modif D. Grimald le 09/01/2001 (Module ajoute)
C ENTREES : NOMPB  NOM DU PROBLEME
C           SIG    CONTRAINTES
C           SIGN   DERNIERE CONTRAINTE DU VOL i-1
C           OMEGA  VALEUR DU FILTRAGE
C           SIGMAD LIMITE D'ENDURANCE
C           NUVOL  NUMERO DU VOL
C           NPIC   NOMBRE DE CONTRAINTES
C SORTIES : SIG    CONTRAINTES 
C           NPIC   NOMBRE DE CONTRAINTES
C **************************************
      CHARACTER*80 NOMPB
      REAL SIG(*)
C
      WRITE(6,*) 'SOUS PROGRAMME TRANSI0'
C
C CONTROLE QUE LA PREMIERE CONTRAINTE EST DE TYPE PIC
C ---------------------------------------------------
      IF (SIG(1).GT.SIG(2)) GOTO 2
C
        I=1
        DO J=I,NPIC-1
          IF (SIG(J).LE.SIG(J+1)) THEN
            I=J+1  
          ELSE
            GOTO 3
          ENDIF
        ENDDO 
C
 3      CONTINUE
        DO J=I,NPIC
          SIG(J-I+1) = SIG(J)
        ENDDO
        NPIC = NPIC-I+1
C
C CONTROLE QUE LA DERNIERE CONTRAINTE EST DE TYPE VALLEE
C ------------------------------------------------------
 2    IF (NPIC.GE.2) THEN
        DO J=NPIC,2,-1
          IF (SIG(J).GE.SIG(J-1)) THEN
             NPIC=NPIC-1
          ELSE
             RETURN
          ENDIF
        ENDDO 
      ENDIF
      RETURN
      END
C --------------------------------------------------------------
      SUBROUTINE TRANSI3(NOMPB,SIG,SIG1,OMEGA,SIGMAD,NUVOL,NPIC)
C --------------------------------------------------------------
C TRANSITION ENTRE SEQUENCE 
C ENTREES : NOMPB  NOM DU PROBLEME
C           SIG    CONTRAINTES
C           SIG1   PREMIERE CONTRAINTE DU VOL 1
C           OMEGA  VALEUR DU FILTRAGE
C           SIGMAD LIMITE D'ENDURANCE
C           NUVOL  NUMERO DU VOL
C           NPIC   NOMBRE DE CONTRAINTES
C SORTIES : SIG    CONTRAINTES 
C           NPIC   NOMBRE DE CONTRAINTES
C **************************************
      CHARACTER*80 NOMPB
      REAL SIG(*)
C
      WRITE(6,*) 'SOUS PROGRAMME TRANSI3'
C
C RECHERCHE DE LA PREMIERE ALTERNANCE SIGNIFICATIVE
C -------------------------------------------------
      IF(SIG1.NE.-9999.) THEN
        S2 = SIG1
        DO I=NPIC,1,-1
          S1 = SIG(I)
          SEUIL = FSEUIL(S1,S2,OMEGA,SIGMAD)
          IF(ABS(S1-S2).GE.SEUIL) GOTO 1
        ENDDO
        WRITE(6,*) 'VOL NUMERO =',NUVOL,' VIDE'
        NPIC = 0
        GOTO 2
C
    1   CONTINUE
        NPIC = I
      ENDIF  
C
    2 CONTINUE
      RETURN
      END
C ----------------------------------------
      SUBROUTINE RMS(SIG,SMAXV,NPIC,IECRI)
C ----------------------------------------
C CALCUL DE LA MOYENNE QUADRATIQUE ET DU RAPPORT DE CRETE
C ENTREES : SIG   CONTRAINTES DU VOL
C           SMAXV CONTRAINTE MAXIMALE DU VOL
C           NPIC  NOMBRE DE CONTRAINTES
C           IECRI FLAG D'ECRITURE
C *******************************
      REAL SIG(*)
C 
      WRITE(6,*) 'SOUS PROGRAMME RMS'
C
C CALCUL DE LA MOYENNE QUADRATIQUE
C --------------------------------
      S = 0.
      DO I=1,NPIC
       S = S+SIG(I)**2
      ENDDO
      VRMS = SQRT(S/NPIC)
C
C CALCUL DU RAPPORT DE CRETE
C --------------------------
      RAPCRE = SMAXV/VRMS
C
   10 FORMAT(1X,':--------------------------------:----------------:----
     &----:-------:')
   20 FORMAT(1X,': ',A30,' : ',F14.5,' : ',A6,' : ',A5,' :') 
      IF(IECRI.EQ.1) THEN
        WRITE(3,20) 'CONTRAINTE QUADRATIQUE MOYENNE',VRMS,' ',' '
        WRITE(3,20) 'RAPPORT DE CRETE',RAPCRE,' ',' '
        WRITE(3,10)
      ENDIF
C
      RETURN
      END
C -------------------------------------
      SUBROUTINE VALIDE(NOMPB,SIG,NPIC)
C -------------------------------------
C SUPPRESSION DES NIVEAUX EGAUX OU INTERMEDIAIRES
C ENTREES : NOMPB NOM DU PROBLEME
C           SIG   CONTRAINTES DU VOL
C           NPIC  NOMBRE DE CONTRAINTES
C *************************************
      CHARACTER*80 NOMPB
      REAL SIG(*)
C
      EPS = 1E-6
C      WRITE(6,*) 'SOUS PROGRAMME VALIDE'
C
C SUPPRESSION DES NIVEAUX EGAUX OU INTERMEDIAIRES
C -----------------------------------------------
      I = 0
    1 CONTINUE
      I = I+1
    2 CONTINUE
      IF(I.LE.NPIC-2) THEN
        PENTE = (SIG(I+1)-SIG(I))*(SIG(I+2)-SIG(I+1))
        IF(PENTE.GE.0.) THEN
C
C SUPPRESSION DU RANG I+1
C -----------------------
          J = I+2
          DO K=J,NPIC
            SIG(K-1) = SIG(K)
          ENDDO
          NPIC = NPIC-1
          GOTO 2
        ENDIF
        GOTO 1
      ENDIF
C
C     K = -10
C     DO I=1,NPIC/10
C       K = K+10
C       WRITE(6,'(10(F10.5,1X))') (SIG(J+K),J=1,10)
C     ENDDO
C     WRITE(6,'(10(F10.5,1X))') (SIG(NPIC/10*10+L),L=1,NPIC-NPIC/10*10)
C
      RETURN
      END
c=========================================================================
c Library libfat
c=========================================================================

C -------------------------
      FUNCTION IFFIN(CHAIN)
C -------------------------
C RECHERCHE DU DERNIER CARACTERE NON BLANC D'UNE CHAINE
C IL FAUT AVOIR INITIALISER CHAIN A ''
C ENTREE : CHAIN : CHAINE DE CARACTERES
C ************************************* 
      CHARACTER*(*) CHAIN
      IFFIN = 0
      DO I=LEN(CHAIN),1,-1
        IF (CHAIN(I:I).NE.' ') THEN
          IFFIN  = I
          GOTO 1
        ENDIF
      ENDDO   
    1 CONTINUE
      RETURN
      END

C -------------------------
      FUNCTION IFDEB(CHAIN)
C -------------------------
C RECHERCHE DU PREMIER CARACTERE NON BLANC D'UNE CHAINE
C PARAMETRE D'ENTREE :
C CHAIN : CHAINE DE CARACTERES
C **************************** 
      CHARACTER*(*) CHAIN
      IFDEB = 0
      DO I=1,LEN(CHAIN)
        IF (CHAIN(I:I).NE.' ') THEN
          IFDEB  = I
          GOTO 1
        ENDIF
      ENDDO
    1 CONTINUE
      RETURN
      END
C -------------------------------------
      SUBROUTINE ERROR(NOMPB,MESSAG,IU)
C -------------------------------------
C CREATION DU FICHIER nompb.erreurs - ARRET DE LA PROCEDURE
C ENTREES : NOMPB  NOM DU PROBLEME
C           MESSAG MESSAGE D'ERREUR
C           UNITE DU FICHIER prefixe.erreurs
C ******************************************
      CHARACTER*80 NOMPB

	CHARACTER*(*) MESSAG
      CHARACTER*80 NOMF,FIN
C
      WRITE(6,'(A)')' SOUS PROGRAMME ERROR'
C
      FIN = 'erreurs'
      CALL RACINE(NOMPB,FIN,NOMF)
      OPEN(IU,FILE=NOMF,FORM='FORMATTED',ACCESS='APPEND',
     &     STATUS='UNKNOWN',ERR=900)
      WRITE(IU,'(A)')
      WRITE(IU,'(A)') ' MESSAGE D''ERREUR'
      WRITE(IU,'(A)') ' -----------------'
      WRITE(IU,'(A)') MESSAG
      WRITE(6,'(A)')  MESSAG(1:IFFIN(MESSAG))
      STOP
C
  901 FORMAT(1X,'ERREUR A L''OUVERTURE DU FICHIER ',A)
  900 WRITE(6,901) NOMF
      STOP
      END
C -----------------------------------------------------------
      REAL FUNCTION FINT(NOMPB,LIBELLE,A,X,Y,IU,NPT,N,ICROIS)
C -----------------------------------------------------------
C FONCTION INTERPOLATION -LINEAIRE-PARABOLIQUE-CUBIQUE-
C CE S/P PERMET L INTERPOLATION SUR UNE COURBE UNIQUE DONT LES TABLEAUX
C DES X ET DES Y SONT CARACTERISES PAR DES NOMS DIFFERENTS.
C LES ABSCISSES DOIVENT ETRE OBLIGATOIREMENT CROISSANTES ET DEUX ABSCIS
C SES CONSECUTIVES NE PEUVENT PAS ETRE EGALES
C INTERPOLATION LINEAIRE   1 POINT  AVANT - 1 POINT  APRES
C INTERPOLATION PARABOL.   2 POINTS AVANT - 1 POINT  APRES
C INTERPOLATION CUBIQUE    2 POINTS AVANT - 2 POINTS APRES SAUF DEBUT ET FIN
C ENTREES : NOMPB   NOM DU PROBLEME
C           LIBELLE NOM DE LA SUITE X
C           A       ARGUMENT D INTERPOLATION
C           X       ABSCISSES
C           Y       ORDONNEES
C           IU      UNITE DU FICHIER prefixe.erreurs
C           NPT     NOMBRE DE POINTS X,Y
C           N       DEGRE D INTERPOLATION =1 LINEAIRE =2 PARABOLIQUE =3 CUBIQUE
C           ICROIS  =0 TEST SUR LA MONOTONIE CROISSANTE DE X
C **********************************************************
      CHARACTER*80  MESSAG
      CHARACTER*80 NOMPB

	CHARACTER*(*) LIBELLE
      REAL X(*),Y(*),Z(3)
C
C     WRITE(6,*) 'SOUS PROGRAMME FINT'
      EPS = 1E-6
C
      IF(ICROIS.EQ.0) CALL CROISS(NOMPB,LIBELLE,X,IU,NPT)
      DO I=1,NPT
        IF(ABS(A-X(I)).LE.EPS) THEN
          FINT = Y(I)
          RETURN
        ELSE IF(A-X(I).LT.0.) THEN
          GOTO 50
        ENDIF
      ENDDO
      WRITE(6,*) 'EXTRAPOLATION',A,X(NPT)
C
C*Modif - D. Grimald - 13/10/00 - Prise en compte des discontinuite K(a)
ccc50 IF(I-(N+2)/2.GT.0) THEN
ccc     I = I-(N+2)/2
C
 50   M = N
 51   IF(I-(M+2)/2.GT.0) THEN
C*Modif - 
        I = I-(M+2)/2
      ELSE
        I = 1
      ENDIF
C
C*Modif - D. Grimald - 13/10/00 - Prise en compte des discontinuite K(a)
C TESTS SUR LES ABSCISSES DISTINCTES
C ----------------------------------
      DO J=1,M
        L=I+J
        IF (((X(L)-X(I)).LT.EPS).AND.(M.EQ.1)) THEN
          FINT=Y(I) + (Y(I+1)-Y(I))/2.
          WRITE(MESSAG,'(1X,A)')
     &      ' INTERPOLATION ENTRE DEUX ABS IDENTIQUES'
          CALL ERROR(NOMPB,MESSAG,IU)
        ELSE IF ((X(L)-X(I)).LT.EPS) THEN
          WRITE(6,'(1X,A)')
     &  'S/P FINT : ATTENTION : INTERPOLATION ENTRE DEUX ABS IDENTIQUES'
          WRITE(6,'(24X,A,I1)') ' REDUCTION DEGRES INTERPOLATION A : ',M
          I=I+(M+2)/2
          M=M-1
          GOTO 51
        ENDIF
      ENDDO
C
C CALCUL
C ------
cccc  M = N
C*Modif - 
      DO J=1,M
        L    = I+J
        Z(J) = (A-X(I))*(Y(L)-Y(I))/(X(L)-X(I))+Y(I)
      ENDDO
C
   60 CONTINUE
      IF(M-1.GT.0) THEN
C
C N PLUS GRAND QUE 1
C ------------------
        M = M-1
        I = I+1
        DO J=1,M
          L      = I+J
          Z(J+1) = (A-X(I))*(Z(J+1)-Z(1))/(X(L)-X(I))+Z(1)
        ENDDO
        DO J=1,M
          Z(J) = Z(J+1)
        ENDDO
        GOTO 60
      ENDIF
C
C SORTIE
C ------
      FINT = Z(1)
      RETURN
      END
C -------------------------------------------------------
      SUBROUTINE DEBUT(NOMPB,NATURE,XLEG,YLEG,X,Y,NBP,NS)
C -------------------------------------------------------
C FICHIER DE TRACE
C ENTREES : NOMPB     NOM DU PROBLEME
C           NATURE    NATURE DU TRACE
C           XLEG,YLEG LEGENDES
C           X,Y       POINTS ENTRES
C           NBP       NOMBRE DE POINTS PAR COURBE
C           NS        UNITE
C**************************
      CHARACTER*80 NOMPB

	CHARACTER*(*) NATURE,XLEG,YLEG
      CHARACTER*80  NOMTR
      REAL X(*),Y(*)
C
      WRITE(6,*) 'SOUS PROGRAMME DEBUT'
C
  306 FORMAT('.fct cree fonction nom "',A,'" ')
  308 FORMAT('texte x "',A,'"  y "',A,'" ')
  310 FORMAT('cree valeur x u')
  312 FORMAT('cree valeur y u')
  314 FORMAT('couples')
  316 FORMAT(I5,1X,E13.6)
  318 FORMAT('genere')
C
      NOMTR=NATURE(IFDEB(NATURE):IFFIN(NATURE))//' Nom pb='//
     &      NOMPB(IFDEB(NOMPB):IFFIN(NOMPB))
      WRITE(NS,306) NOMTR(IFDEB(NOMTR):IFFIN(NOMTR))
C
      WRITE(NS,308) XLEG(IFDEB(XLEG):IFFIN(XLEG)),
     &              YLEG(IFDEB(YLEG):IFFIN(YLEG))
      WRITE(NS,310)
      WRITE(NS,314)
      DO I=1,NBP
        WRITE(NS,316) I,X(I)
      ENDDO
      WRITE(NS,312)
      WRITE(NS,314)
      DO I=1,NBP
        WRITE(NS,316) I,Y(I)
      ENDDO
      WRITE(NS,318)
C
      RETURN
      END
C -------------------------------------------------------
      SUBROUTINE LECABRE(NOMPB,IMOD,IFIT,IU1,IU2,IU3,IU4)
C -------------------------------------------------------
C LECTURE DES ABREMOD ET ABRETOG DE LA BANQUE 
C ET ECRITURE DANS LE FICHIER prefixe.DOSSIER
C ENTREES : NOMPB  NOM DU PROBLEME
C           IMOD   =1 MODELE E.F.
C           IFIT   =1 AFFICHAGE AUTOMATIQUE DES TABLEAUX
C           IU1    UNITE DU FICHIER BANQUE
C           IU2    UNITE DU FICHIER ERREURS
C           IU3    UNITE DU FICHIER DOSSIER
C           IU4    UNITE DU FICHIER MATERIAUX
C *******************************************
      PARAMETER(MAXFIS=60,MAXMAT=20,MAXROW=60,MAXCOL=21)
C
      CHARACTER*80 NOMPB
      CHARACTER*220 FORMA1,FORMA2,FORMA3,FORMA4(MAXCOL)
      CHARACTER*120 CARTE,CHAINE(10),ABRE(200),LIBELLE(200),NOM(200)
      CHARACTER*80  EQUAT,MESSAG,LABELR(MAXMAT),LABELC(MAXMAT),
     &              LIBCEL(MAXMAT,0:MAXROW,0:MAXCOL),TITRE(MAXMAT)
      REAL VALEUR(MAXFIS,10)
      INTEGER ITAIL(MAXMAT,MAXCOL),NR(MAXMAT),NC(MAXMAT)
C
      WRITE(6,*) 'SOUS PROGRAMME LECABRE'
C
   10 FORMAT(1X,':------------------------------------------------------
     &----------------------------:-------------------------------------
     &---------------:')
   20 FORMAT(1X,': ',A80,' : ',A50,' :')
   30 FORMAT(1X,': ',A80,' : ',E14.6,A36,' :')
      K      = 0
      INDICE = 0
      IM     = 0
C
C LECTURE DES ABREMOD ET ABRETOG
C ------------------------------
      REWIND(IU1)
    1 READ(IU1,'(A)',END=100,ERR=900) CARTE
C     WRITE(6,'(A)') CARTE(IFDEB(CARTE):IFFIN(CARTE))
      IF(INDEX(CARTE,'ABREFAT').NE.0) THEN      
        INDICE = 1
        I1 = INDEX(CARTE(1:120),'"')
        I2 = INDEX(CARTE(I1+1:120),'"')
        I3 = INDEX(CARTE(I1+I2+1:120),'"')
        I4 = INDEX(CARTE(I1+I2+I3+1:120),'"')
        EQUAT = CARTE(I1+I2+I3+1:I1+I2+I3+I4-1)
      ENDIF
      IF(INDEX(CARTE,'ABRETIT').NE.0) THEN
        K = K+1 
        I1 = INDEX(CARTE(1:120),'"')
        I2 = INDEX(CARTE(I1+1:120),'"')
        I3 = INDEX(CARTE(I1+I2+1:120),'"')
        I4 = INDEX(CARTE(I1+I2+I3+1:120),'"')
        ABRE(K)    = CARTE(I1+I2+I3+1:I1+I2+I3+I4-1)
        LIBELLE(K) = 'RIEN'
      ENDIF
      IF((INDEX(CARTE,'ABREMOD').NE.0).OR.
     &   (INDEX(CARTE,'ABRETOG').NE.0)) THEN
        K = K+1 
        I1 = INDEX(CARTE(1:120),'"')
        I2 = INDEX(CARTE(I1+1:120),'"')
        I3 = INDEX(CARTE(I1+I2+1:120),'"')
        I4 = INDEX(CARTE(I1+I2+I3+1:120),'"')
        I5 = INDEX(CARTE(I1+I2+I3+I4+1:120),'!')
        NOM(K)     = CARTE(I1+1:I1+I2-1)
        ABRE(K)    = CARTE(I1+I2+I3+1:I1+I2+I3+I4-1)
        LIBELLE(K) = CARTE(I1+I2+I3+I4+I5+1:120)
      ENDIF
C
      IF(INDEX(CARTE,'ABREFIT').NE.0) THEN      
        ICOL = 0
        IROW = 0
        IT   = 0
        IM   = IM+1
        I1   = INDEX(CARTE(1:120),'"')
        I2   = INDEX(CARTE(I1+1:120),'"')
        I3   = INDEX(CARTE(I1+I2+1:120),'"')
        I4   = INDEX(CARTE(I1+I2+I3+1:120),'/')
        I5   = INDEX(CARTE(I1+I2+I3+I4+1:120),'/')
        I6   = INDEX(CARTE(I1+I2+I3+I4+I5+1:120),'/')
        I7   = INDEX(CARTE(I1+I2+I3+I4+I5+I6+1:120),'/')
        I8   = INDEX(CARTE(I1+I2+I3+I4+I5+I6+I7+1:120),'/')
        I9   = INDEX(CARTE(I1+I2+I3+I4+I5+I6+I7+I8+1:120),'/')
        I10  = INDEX(CARTE(I1+I2+I3+I4+I5+I6+I7+I8+I9+1:120),'/')
        IF(IFDEB(CARTE(I1+I2+I3+1:I1+I2+I3+I4-1)).NE.0) THEN
          READ(CARTE(I1+I2+I3+1:I1+I2+I3+I4-1),'(A)',ERR=900) TITRE(IM)
        ELSE
          TITRE(IM) = 'SANS TITRE'
        ENDIF
        READ(CARTE(I1+I2+I3+I4+I5+1:I1+I2+I3+I4+I5+I6-1),*,ERR=900)
     &    NR(IM)
        READ(CARTE(I1+I2+I3+I4+I5+I6+1:I1+I2+I3+I4+I5+I6+I7-1),*,
     &       ERR=900) NC(IM)
        IF(IFDEB(CARTE(I1+I2+I3+I4+I5+I6+I7+1:
     &    I1+I2+I3+I4+I5+I6+I7+I8-1)).NE.0) THEN
          READ(CARTE(I1+I2+I3+I4+I5+I6+I7+1:
     &         I1+I2+I3+I4+I5+I6+I7+I8-1),'(A)',ERR=900) LABELR(IM)
          IF(INDEX(LABELR(IM),'rien').NE.0) LABELR(IM) = ' '
          DO I=1,NR(IM)
            WRITE(LIBCEL(IM,I,0),'(A,I3)') 
     &        LABELR(IM)(IFDEB(LABELR(IM)):IFFIN(LABELR(IM))),I
          ENDDO
        ELSE
          LABELR(IM) = ' '
          DO I=1,NR(IM)
            WRITE(LIBCEL(IM,I,0),'(I2)') I
          ENDDO
        ENDIF
        IF(IFDEB(CARTE(I1+I2+I3+I4+I5+I6+I7+I8+1:
     &    I1+I2+I3+I4+I5+I6+I7+I8+I9-1)).NE.0) THEN
          READ(CARTE(I1+I2+I3+I4+I5+I6+I7+I8+1:
     &         I1+I2+I3+I4+I5+I6+I7+I8+I9-1),'(A)',ERR=900) LABELC(IM)
          IF(INDEX(LABELC(IM),'rien').NE.0) LABELC(IM) = ' '
          DO I=1,NC(IM)
            WRITE(LIBCEL(IM,0,I),'(A,I3)') 
     &        LABELC(IM)(IFDEB(LABELC(IM)):IFFIN(LABELC(IM))),I
          ENDDO
        ELSE
          LABELC(IM) = ' '
          DO I=1,NC(IM)
            WRITE(LIBCEL(IM,0,I),'(I2)') I
          ENDDO
        ENDIF
        READ(CARTE(I1+I2+I3+I4+I5+I6+I7+I8+I9+1:I1+I2+I3+I4+I5+I6+I7+I8+
     &             I9+I10-1),*) ITAILS
        DO I=1,MAXMAT
          DO J=1,MAXCOL
            ITAIL(I,J) = ITAILS
          ENDDO
        ENDDO
      ENDIF
C
      IF(INDEX(CARTE,'ABREFIC').NE.0) THEN      
        ICOL = ICOL+1
        I1   = INDEX(CARTE(1:120),'"')
        I2   = INDEX(CARTE(I1+1:120),'"')
        I3   = INDEX(CARTE(I1+I2+1:120),'"')
        I4   = INDEX(CARTE(I1+I2+I3+1:120),'/')
        I5   = INDEX(CARTE(I1+I2+I3+I4+1:120),'/')
        IF(I5.EQ.0) I5 = INDEX(CARTE(I1+I2+I3+I4+1:120),'"')
        IF(IFDEB(CARTE(I1+I2+I3+1:I1+I2+I3+I4-1)).EQ.0) THEN
          LIBCEL(IM,0,ICOL) = ' '
        ELSE   
          READ(CARTE(I1+I2+I3+1:I1+I2+I3+I4-1),'(A)',ERR=900) 
     &      LIBCEL(IM,0,ICOL)
        ENDIF
        READ(CARTE(I1+I2+I3+I4+1:I1+I2+I3+I4+I5-1),*) ITAIL(IM,ICOL)
      ENDIF
C
      IF(INDEX(CARTE,'ABREFIL').NE.0) THEN      
        IROW = IROW+1
        I1   = INDEX(CARTE(1:120),'"')
        I2   = INDEX(CARTE(I1+1:120),'"')
        I3   = INDEX(CARTE(I1+I2+1:120),'"')
        I4   = INDEX(CARTE(I1+I2+I3+1:120),'"')
        IF(IFDEB(CARTE(I1+I2+I3+1:I1+I2+I3+I4-1)).NE.0) THEN
          READ(CARTE(I1+I2+I3+1:I1+I2+I3+I4-1),'(A)',ERR=900) 
     &    LIBCEL(IM,IROW,0)
        ELSE
          LIBCEL(IM,IROW,0) = ' '
        ENDIF
      ENDIF
C
      IF((INDEX(CARTE,'ABREFIX').NE.0).AND.(IM.NE.0)) THEN      
        IT   = IT+1
        IR   = INT((IT-1)/NC(IM))+1
        IC   = IT-NC(IM)*(IR-1)
        I1   = INDEX(CARTE(1:120),'"')
        I2   = INDEX(CARTE(I1+1:120),'"')
        I3   = INDEX(CARTE(I1+I2+1:120),'"')
        I4   = INDEX(CARTE(I1+I2+I3+1:120),'"')
        IF((IR.LE.NR(IM)).AND.(IC.LE.NC(IM))) READ(CARTE(I1+I2+I3+1:
     &    I1+I2+I3+I4-1),'(A)',ERR=900) LIBCEL(IM,IR,IC)
      ENDIF
C
      GOTO 1
C
  100 CONTINUE
      IF(INDICE.EQ.1) THEN
        WRITE(IU3,'(A)') 
        WRITE(IU3,'(A)') 
        WRITE(IU3,'(A)') ' ================='
        WRITE(IU3,'(A)') ' DONNEES DU MODELE' 
        WRITE(IU3,'(A)') ' ================='
        WRITE(IU3,'(1X,A17,A55)') 'MODELE INITIAL = ',
     &    EQUAT(IFDEB(EQUAT):80)
        WRITE(IU3,'(A)') 
C
        DO I=1,K
          IF(INDEX(LIBELLE(I),'RIEN').NE.0) THEN
            WRITE(IU3,10) 
            WRITE(IU3,20) ABRE(I),' '
            WRITE(IU3,10) 
          ELSE
            WRITE(IU3,20) 
     &      LIBELLE(I)(IFDEB(LIBELLE(I)):120),ABRE(I)
C
C LECTURE DES DONNES MATERIAUX
C ----------------------------
            IF(INDEX(NOM(I),'%NOMMAT').NE.0) THEN
              IF(IMOD.EQ.1) THEN
                NOMBRE    = 2
                CHAINE(1) = '%YOUN'
                CHAINE(2) = '%POIS'
                CALL LECMAT(NOMPB,CHAINE,ABRE(I),IU4,IU2,1,NOMBRE,
     &                      VALEUR)
                WRITE(IU3,30) 'YOUNG (MPa)',VALEUR(1,1),' '
                WRITE(IU3,30) 'POISSON',VALEUR(1,2),' '
              ELSE IF(IMOD.EQ.2) THEN
                NOMBRE    = 2
                CHAINE(1) = '%LIEL'
                CHAINE(2) = '%POIS'
                CALL LECMAT(NOMPB,CHAINE,ABRE(I),IU4,IU2,1,NOMBRE,
     &                      VALEUR)
                WRITE(IU3,30) 'LIMITE ELASTIQUE (MPa)',VALEUR(1,1),' '
                IF(EQUAT(IFDEB(EQUAT):IFFIN(EQUAT)).EQ.'SC05') THEN
                  WRITE(IU3,30) 'POISSON',VALEUR(1,2),' '
                ENDIF
              ENDIF
            ENDIF
          ENDIF
        ENDDO
        WRITE(IU3,10) 
        WRITE(IU3,'(A)') 
C
        IF(IFIT.NE.1) IM = 0
        DO I=1,IM
          FORMA1 = ' '
          FORMA2 = ' '
          FORMA3 = ' '
          WRITE(IU3,'(A)') 
          WRITE(IU3,'(1X,A)') TITRE(I)
C
          WRITE(FORMA1(1:1),'(A1)') ':'
          WRITE(FORMA2(1:1),'(A1)') ':'
          WRITE(FORMA1(2:26),'(A25)') '------------------------------'
          IF(IFDEB(LABELR(I)).EQ.0) THEN
            WRITE(FORMA2(2:26),'(A25)') ' '
          ELSE
            WRITE(FORMA2(2:26),'(A25)') 
     &        LABELR(I)(IFDEB(LABELR(I)):IFFIN(LABELR(I)))
          ENDIF  
          WRITE(FORMA1(27:27),'(A1)') ':'
          WRITE(FORMA2(27:27),'(A1)') ':'
          IPOS   = 26
          DO J=1,NC(I)
            ITA        = ITAIL(I,J)/.8
            IF         = MAX(7,ITA)
            DO N=1,IF 
              WRITE(FORMA1(IPOS+1+N:IPOS+1+N),'(A1)') '-'
            ENDDO
            IF(IF.LE.9)  WRITE(FORMA4(J),'(A2,I1,A1)') '(A',IF,')'
            IF(IF.GE.10) WRITE(FORMA4(J),'(A2,I2,A1)') '(A',IF,')'
            IF(IFDEB(LIBCEL(I,0,J)).EQ.0) THEN
              WRITE(FORMA2(IPOS+2:IPOS+IF+1),
     &            FORMA4(J)(IFDEB(FORMA4(J)):IFFIN(FORMA4(J)))) ' '
            ELSE
              WRITE(FORMA2(IPOS+2:IPOS+IF+1),
     &          FORMA4(J)(IFDEB(FORMA4(J)):IFFIN(FORMA4(J)))) 
     &          LIBCEL(I,0,J)(IFDEB(LIBCEL(I,0,J)):IFFIN(LIBCEL(I,0,J)))
            ENDIF
            WRITE(FORMA1(IPOS+IF+2:IPOS+IF+2),'(A1)') ':'
            WRITE(FORMA2(IPOS+IF+2:IPOS+IF+2),'(A1)') ':'
            IPOS = IPOS+IF+1
          ENDDO
          WRITE(IU3,'(1X,A)') FORMA1(IFDEB(FORMA1):IFFIN(FORMA1))
          WRITE(IU3,'(1X,A)') FORMA2(IFDEB(FORMA2):IFFIN(FORMA2))
          WRITE(IU3,'(1X,A)') FORMA1(IFDEB(FORMA1):IFFIN(FORMA1))
C
          IT   = 0
          DO J1=1,NR(I)
            IPOS = 1
            WRITE(FORMA3(1:1),'(A1)') ':'
            WRITE(FORMA3(2:26),'(A25)') 
     &       LIBCEL(I,J1,0)(IFDEB(LIBCEL(I,J1,0)):IFFIN(LIBCEL(I,J1,0)))
            WRITE(FORMA3(27:27),'(A1)') ':'
            IPOS = 27
C
            DO J2=1,NC(I)
              IT          = IT+1
              IR          = INT((IT-1)/NC(I))+1
              IC          = IT-NC(I)*(IR-1)
              ITA         = ITAIL(I,J2)/.8
              IFA         = MAX(7,ITA)
              IF(IFDEB(LIBCEL(I,J1,J2)).EQ.0) THEN
                WRITE(FORMA3(IPOS+1:IPOS+IFA),FORMA4(J2)) ' '
              ELSE
                WRITE(FORMA3(IPOS+1:IPOS+IFA),FORMA4(J2))
     &    LIBCEL(I,J1,J2)(IFDEB(LIBCEL(I,J1,J2)):IFFIN(LIBCEL(I,J1,J2)))
              ENDIF
              WRITE(FORMA3(IPOS+IFA+1:IPOS+IFA+1),'(A1)') ':'
              IPOS = IPOS+IFA+1
            ENDDO 
            WRITE(IU3,'(1X,A)') FORMA3(IFDEB(FORMA3):IFFIN(FORMA3))
          ENDDO
          WRITE(IU3,'(1X,A)') FORMA1(IFDEB(FORMA1):IFFIN(FORMA1))
        ENDDO 
        WRITE(IU3,'(1X,A)') ' '
      ENDIF
      RETURN
C
  901 FORMAT(1X,'ERREUR A LA LECTURE DU FICHIER banque')
  900 WRITE(MESSAG,901)
      CALL ERROR(NOMPB,MESSAG,IU2)
      END








C -------------------------------------------------------------------
      SUBROUTINE FIN(NS,INUM,IVAL,ITC,INB,IGL,ICL,ITS,ICAX,ICCA,ILEG,
     &               IEFF,IGRA,IGRI)
C ----------------------------------
C FIN DU FICHIER DE TRACE
C ENTREES : NS     UNITE
C           INUM   AFFICHAGE DU NUMERO DE COURBE
C           IVAL   AFFICHAGE DES VALEURS
C           ITC    TAILLE DES CARACTERES
C           INB    COULEUR DE VISUALISATION
C           IGL    TYPE DE TRAIT
C           ICL    COULEUR DE LA LIGNE
C           ITS    TYPE DE SYMBOLE
C           ICAX   COULEUR DES AXES
C           ICCA   COULEUR DES CARACTERES
C           ILEG   AFFICHAGE DU NOM DE LA GRANDEUR
C           IEFF   EFFACAGE AVANT DE TRACER
C           IGRA   AFFICHAGE DES ECHELLES
C           IGRI   AFFICHAGE DE LA GRILLE
C ***************************************
      WRITE(6,*) 'SOUS PROGRAMME FIN'
C
C 410 FORMAT('titre "',A,'" ligne 1 taille 3')
C 412 FORMAT('titre "Probleme : ',A,'" ligne 2 taille 3')
  414 FORMAT('affiche numero',1X,I2)
  416 FORMAT('affiche valeurs',1X,I2)
  418 FORMAT('grap tc',1X,I2)
  420 FORMAT('grap nb',1X,I2)
  422 FORMAT('grap gl',1X,I2)
  424 FORMAT('grap cl',1X,I2)
  425 FORMAT('grap ts',1X,I2)
  426 FORMAT('grap cax',1X,I2)
  428 FORMAT('grap cca',1X,I2)
  430 FORMAT('grap legende',1X,I2)
  432 FORMAT('grap efface',1X,I2)
  434 FORMAT('grap graduation',1X,I2)
  436 FORMAT('grille',1X,I2)
C
      WRITE(NS,414) INUM
      WRITE(NS,416) IVAL
      WRITE(NS,418) ITC
      WRITE(NS,420) INB
      WRITE(NS,422) IGL
      WRITE(NS,424) ICL
      WRITE(NS,425) ITS
      WRITE(NS,426) ICAX
      WRITE(NS,428) ICCA
      WRITE(NS,430) ILEG
      WRITE(NS,432) IEFF
      WRITE(NS,434) IGRA
      WRITE(NS,436) IGRI
C            
C     J     = 0 
C     VI(1) = 'vi'
C     VI(2) = ' '
C     NB = NCOURB/22
C     NR = NCOURB-NB*22
C     DO J=1,NB
C       DO I=1,22
C         WRITE(CHNUM,'(I3)') NDEB-1+I+(J-1)*22
C         VI(J) = VI(J)(IFDEB(VI(J)):IFFIN(VI(J)))//CHNUM
C       ENDDO
C       VI(J) = VI(J)(IFDEB(VI(J)):IFFIN(VI(J)))//' $'
C       WRITE(NS,'(A)') VI(J)(IFDEB(VI(J)):IFFIN(VI(J)))
C     ENDDO
C     DO I=1,NR
C       WRITE(CHNUM,'(I3)') 22*NB+NDEB-1+I
C       VI(NB+1) = VI(NB+1)(IFDEB(VI(NB+1)):IFFIN(VI(NB+1)))//CHNUM
C     ENDDO
C     IF(NR.GE.1) THEN
C       WRITE(NS,'(A)') VI(NB+1)(IFDEB(VI(NB+1)):IFFIN(VI(NB+1)))
C     ENDIF
C
      RETURN
      END
C -------------------------------------
      SUBROUTINE RACINE(NOMPB,FIN,NOMF)
C -------------------------------------
C ENTREES : NOMPB  NOM DU PROBLEME
C           FIN    SUFFIXE
C SORTIE  : NOMF   NOMPB.SUFFIXE
C ******************************
      CHARACTER*80 NOMPB

	CHARACTER*(*) FIN,NOMF
      CHARACTER*1 POINT
C
      POINT = '.'
C
      NOMF = NOMPB(IFDEB(NOMPB):IFFIN(NOMPB))//POINT//FIN
      RETURN
      END
C ---------------------------------------
      SUBROUTINE STOP(NOMPB,CHAINE,IFLAG)
C ---------------------------------------
C TEST SUR L'EXISTENCE DU FICHIER prefixe.fissur
C ENTREES : NOMPB   NOM DU PROBLEME
C           CHAINE  SUFFIXE DU FICHIER
C SORTIES : IFLAG   =0 PAS DE PB =1 ARRET
C **************************************
      CHARACTER*80 NOMPB

	CHARACTER*(*) CHAINE
      CHARACTER*80  FIN,NOMF
      LOGICAL LEXIST
C
      WRITE(6,*) 'SOUS PROGRAMME STOP'
C
      FIN = CHAINE(IFDEB(CHAINE):IFFIN(CHAINE))
      CALL RACINE(NOMPB,CHAINE,NOMF)
      INQUIRE(FILE=NOMF,EXIST=LEXIST)
      WRITE(6,*) 'EXISTENCE DU FICHIER =',NOMF(IFDEB(NOMF):IFFIN(NOMF))
      IF(LEXIST) THEN
        IFLAG = 1
      ELSE
        IFLAG = 0
      ENDIF
      WRITE(6,*) 'IFLAG =',IFLAG
C
      RETURN
      END
C --------------------------------
      SUBROUTINE ENTETE(IU,COMMEN)
C --------------------------------
C ECRITURE DE L'ENTETE DANS LE FICHIER prefixe.dossier
C ENTREES : IU      UNITE DU FICHIER prefixe.dossier
C           COMMEN  COMMENTAIRE
C *****************************
      CHARACTER*(*) COMMEN
      CHARACTER*80  DATE
C
      WRITE(6,*) 'SOUS PROGRAMME ENTETE'
C
   10 FORMAT(1X,'DATE D''EXECUTION : ',A)
      WRITE(IU,'(A)')
      WRITE(IU,'(1X,A)') 'SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
     &SSSSSSSSSSSSSSSSSSSSSSS'
      WRITE(IU,'(1X,A)') 'S                                                 
     &                      S'
      WRITE(IU,'(1X,A)') 'S   SSSSSSSS  SSSSSSSS  SSSSSSSS  SS  SSSSSSSS
     &  SS    SS  SSSSSSSS  S'
      WRITE(IU,'(1X,A)') 'S   SS        SS    SS     SS     SS  SS    SS
     &  SS    SS  SS        S'
      WRITE(IU,'(1X,A)') 'S   SS        SS    SS     SS     SS  SS      
     &  SS    SS  SS        S'
      WRITE(IU,'(1X,A)') 'S   SSSSS     SSSSSSSS     SS     SS  SS  SSSS
     &  SS    SS  SSSSSS    S'
      WRITE(IU,'(1X,A)') 'S   SS        SS    SS     SS     SS  SS    SS
     &  SS    SS  SS        S'
      WRITE(IU,'(1X,A)') 'S   SS        SS    SS     SS     SS  SS    SS
     &  SS    SS  SS        S'
      WRITE(IU,'(1X,A)') 'S   SS        SS    SS     SS     SS  SSSSSSSS
     &  SSSSSSSS  SSSSSSSS  S'
      WRITE(IU,'(1X,A)') 'S                                                 
     &                      S'
      WRITE(IU,'(1X,A)') 'SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
     &SSSSSSSSSSSSSSSSSSSSSSS'
      WRITE(IU,'(1X,A)')
      WRITE(IU,'(1X,A,1X,A)') 'VERSION PROTO - ',COMMEN
      WRITE(IU,'(1X,A)')
      CALL FDATE(DATE)
      WRITE(IU,'(1X,A)')
      WRITE(IU,10) DATE(IFDEB(DATE):IFFIN(DATE))
      WRITE(IU,'(1X,A)')
      WRITE(IU,'(1X,A)')
C
      RETURN
      END

C -----------------------------------------------
      SUBROUTINE CROISS(NOMPB,LIBELLE,X,IU,NBPAS)
C -----------------------------------------------
C VERIFICATION DE LA MONOTONIE CROISSANTE DE X
C ENTREES : NOMPB   NOM DU PROBLEME
C           NATURE  LIBELLE
C           X       SUITE DE POINTS ENTRES
C           IU      UNITE DU FICHIER prefixe.erreurs
C           NBPAS   NOMBRE DE POINTS
C **********************************
      CHARACTER*80 NOMPB

	CHARACTER*(*) LIBELLE
      CHARACTER*80  MESSAG
      REAL X(*)
C
C     WRITE(6,*) 'SOUS PROGRAMME CROISS'
C
      DO I=1,NBPAS-1
        IF(X(I+1).LE.X(I)) THEN
  901     FORMAT(1X,'SUITE DE POINTS ',A,' NON CROISSANTE')
          WRITE(MESSAG,901) LIBELLE(IFDEB(LIBELLE):IFFIN(LIBELLE))
          CALL ERROR(NOMPB,MESSAG,IU)
        ENDIF
      ENDDO
C
      RETURN
      END
C -------------------------------------------------------------------
      SUBROUTINE LECMAT(NOMPB,CHAINE,AMAT,IU1,IU2,NFIS,NOMBRE,VALEUR)
C -------------------------------------------------------------------
C SOUS PROGRAMME DE LECTURE DE LA BASE DE DONNEES MATERIAU
C ENTREES NOMPB  NOM DU PROBLEME
C         AMAT   DESIGNATION MATERIAU
C         CHAINE TABLEAU DE CHAINES
C         IU1    UNITE DU FICHIER MATERIAU
C         IU2    UNITE DU FICHIER prefixe.erreurs
C         NFIS   NUMERO DE LA FISSURE
C         NOMBRE NOMBRE DE VALEURS A RECUPERER
C SORTIE  VALEUR TABLEAU DE VALEURS
C *********************************
      PARAMETER(MAXFIS=60)
C
      CHARACTER*80 NOMF,CHAIRE,CARTE,MESSAG,TOGGLE
      CHARACTER*1 COTE
      CHARACTER*80 NOMPB

	CHARACTER*(*) AMAT,CHAINE(*)
      REAL VALEUR(MAXFIS,*)
C
      WRITE(6,*) 'SOUS PROGRAMME LECMAT'
C
      COTE = '"'
C
      NOMF = AMAT(IFDEB(AMAT):IFFIN(AMAT))
      OPEN(IU1,FILE=NOMF,FORM='FORMATTED',STATUS='UNKNOWN',ERR=900)
      WRITE(6,90) NOMF(IFDEB(NOMF):IFFIN(NOMF))
 90   FORMAT(1X,'FICHIER MATERIAU            = ',A)
C
      DO 50 I=1,NOMBRE
        CHAIRE = CHAINE(I)(IFDEB(CHAINE(I)):IFFIN(CHAINE(I)))//
     &           COTE
        REWIND(IU1)
        ICOEF = 0
C
    1   READ(IU1,'(A)',END=100,ERR=900) CARTE
C
C LECTURE D'UNE CARTE ABRETOG
C ---------------------------
        IF(INDEX(CARTE,'ABRETOG').NE.0) THEN
          L1     = INDEX(CARTE,'"') 
          L2     = INDEX(CARTE(L1+1:80),'"') 
          L3     = INDEX(CARTE(L1+L2+1:80),'"') 
          L4     = INDEX(CARTE(L1+L2+L3+1:80),'"') 
          READ(CARTE(L1+L2+L3+1:L1+L2+L3+L4-1),'(A)') TOGGLE
C
C POSITIONNMENT SUR LA LIGNE SUIVANTE CONTENANT ABRETOG
C -----------------------------------------------------
    2     READ(IU1,'(A)',END=100,ERR=900) CARTE
          IF(INDEX(CARTE,TOGGLE(IFDEB(TOGGLE):IFFIN(TOGGLE))).EQ.0) 
     &    GOTO 2
        ENDIF
C
        IF(((INDEX(CARTE,'ABREMOD').NE.0).OR.
     &     (INDEX(CARTE,'ABREFIX').NE.0))
     &  .AND.(INDEX(CARTE,CHAIRE(IFDEB(CHAIRE):IFFIN(CHAIRE))).NE.0)) 
     &  THEN
          ICOEF = 1
          L1     = INDEX(CARTE,'"') 
          L2     = INDEX(CARTE(L1+1:80),'"') 
          L3     = INDEX(CARTE(L1+L2+1:80),'"') 
          L4     = INDEX(CARTE(L1+L2+L3+1:80),'"') 
          IF(IFDEB(CARTE(L1+L2+L3+1:L1+L2+L3+L4-1)).NE.0) THEN
            READ(CARTE(L1+L2+L3+1:L1+L2+L3+L4-1),*) VALEUR(NFIS,I)
            GOTO 50
          ELSE
C
C ARRET POUR UNE CARTE ABREMOD (DONNEE OBLIGATOIRE)
C -------------------------------------------------
            IF(INDEX(CARTE,'ABREMOD').NE.0) THEN
  902         FORMAT(1X,'DONNEE INEXISTANTE = ',A)
              WRITE(MESSAG,902) CHAIRE(IFDEB(CHAIRE)+1:IFFIN(CHAIRE)-1)
              CALL ERROR(NOMPB,MESSAG,IU2)
            ELSE  
C
C VALEUR -9999. POUR UNE CARTE ABREFIX (DONNEE FACULTATIVE)
C ---------------------------------------------------------
              VALEUR(NFIS,I) = -9999.
              GOTO 50
            ENDIF
          ENDIF
        ENDIF
        GOTO 1
C
  100   CONTINUE
        IF(ICOEF.EQ.0) THEN
  904     FORMAT(1X,'DONNEE INEXISTANTE = ',A) 
          WRITE(MESSAG,904) CHAIRE(IFDEB(CHAIRE)+1:IFFIN(CHAIRE)-1)
          CALL ERROR(NOMPB,MESSAG,IU2)
        ENDIF
        REWIND(IU1)
   50 CONTINUE
C
      RETURN
C
  901 FORMAT(1X,'ERREUR A LA LECTURE DU FICHIER MATERIAU')   
  900 WRITE(MESSAG,901)
      CALL ERROR(NOMPB,MESSAG,IU2)
      END

C -------------------------------------------------
      SUBROUTINE LECCHA(NOMPB,CHAINE,IU1,IU2,CHVAL)
C -------------------------------------------------
C ENTREES : NOMPB  NOM DU PROBLEME
C           CHAINE CHAINE RECHERCHEE
C           IU1    UNITE DU FICHIER
C           IU2    UNITE DU FICHIER prefixe.erreurs
C SORTIES : CHVAL  VALEUR DE L'ABRE CHAINE 
C ****************************************
      CHARACTER*80 NOMPB

	CHARACTER*(*) CHAINE,CHVAL
      CHARACTER*1   COTE
      CHARACTER*80  CARTE,CHAIRE,MESSAG
C
      WRITE(6,*) 'SOUS PROGRAMME LECCHA'
C
      COTE  = '"'
      CHVAL = 'RIEN'
      REWIND(IU1)
    1 READ(IU1,'(A)',ERR=900,END=2) CARTE
      CHAIRE = CHAINE(IFDEB(CHAINE):IFFIN(CHAINE))//COTE
      IF((INDEX(CARTE,'ABRE').NE.0).AND.
     &   (INDEX(CARTE,CHAIRE(IFDEB(CHAIRE):IFFIN(CHAIRE))).NE.0)) THEN
        L1 = INDEX(CARTE,'"')
        L2 = INDEX(CARTE(L1+1:80),'"')
        L3 = INDEX(CARTE(L1+L2+1:80),'"')
        L4 = INDEX(CARTE(L1+L2+L3+1:80),'"')
        READ(CARTE(L1+L2+L3+1:L1+L2+L3+L4-1),'(A)') CHVAL
        GOTO 2
      ENDIF  
      GOTO 1
C
    2 CONTINUE
C
      RETURN
C
  901 FORMAT(1X,'ERREUR A LA LECTURE DU FICHIER BANQUE')   
  900 WRITE(MESSAG,901)
      CALL ERROR(NOMPB,MESSAG,IU2)
      END
C ----------------------------------------------
      SUBROUTINE ECRRF(NOMPB,SRF,IU,NVOL,NOMBRE)
C ----------------------------------------------
C CONSTITUTION DU FICHIER prefixe.trace PARTIE RAIN-FLOW
C ENTREES : NOMPB   NOM DU PROBLEME
C           SRF     CYCLES APRES RAIN-FLOW
C           IU      UNITE DU FICHIER prefixe.trace
C           NVOL    NUMERO DU VOL
C           NOMBRE  NOMBRE DE POINTS
C **********************************
      PARAMETER(MAXSIG=800000,MAXCYC=MAXSIG/2)
C
      CHARACTER*80 NOMPB
      CHARACTER*80  NATURE
      CHARACTER*4   CHVOL 
      REAL SRF(2,*),XTRA(MAXSIG),YTRA(MAXSIG)
C
      WRITE(6,*) 'SOUS PROGRAMME ECRRF'
C
  398 FORMAT('! =========')
  400 FORMAT('! RAIN-FLOW')
C
      WRITE(IU,'(A)')
      WRITE(IU,398)
      WRITE(IU,400)
      WRITE(IU,398)
C
C FICHIER prefixe.trace
C ---------------------
      K = -1
      DO I=1,NOMBRE
        K = K+2
        XTRA(K)   = FLOAT(2*I-1)
        YTRA(K)   = SRF(1,I)
        XTRA(K+1) = FLOAT(2*I)
        YTRA(K+1) = SRF(2,I)
      ENDDO 
      WRITE(CHVOL,'(I4)') NVOL
      NATURE = 'Rain-Flow-Vol-'//CHVOL
      CALL DEBUT(NOMPB,NATURE,'temps','S (MPa)',XTRA,YTRA,K+1,IU)
C
      RETURN
      END
C ------------------------------------------------------------------
      SUBROUTINE RESEAU(NOMPB,VALEUR,IFI,KDECAL,A,B,C,NPTREX,NPTREY)
C ------------------------------------------------------------------
C LECTURE DU RESEAU (A,B,C)
C ENTREES : NOMPB           NOM DU PROBLEME
C           VALEUR          TABLEAUX MATERIAU
C           IFI             NUMERO DE LA FISSURE
C           KDECAL          NOMBRE DE VALEURS MATERIAU LUES AVANT LE RESEAU
C SORTIES : A,B,C           RESEAU (R,da/dN,dK),(Smoy,N,Salt)
C           NPTREX          NOMBRE DE POINTS R RESEAU
C           NPTREY          NOMBRE DE POINTS da/dN RESEAU
C *******************************************************
      PARAMETER(MAXFIS=60,NRESX=5,NRESY=9)
C
      CHARACTER*80 NOMPB
      CHARACTER*120 MESSAG
      REAL A(MAXFIS,*),B(MAXFIS,*),C(MAXFIS,NRESX,*),VALEUR(MAXFIS,*)
      INTEGER NPTREX(*),NPTREY(*),NPTRYR(MAXFIS,NRESX)
C
      WRITE(6,*) 'SOUS PROGRAMME RESEAU'
C
      NPTREX(IFI) = 0
      DO J=1,NRESX
        NPTRYR(IFI,J) = 0
      ENDDO
      DO K=1,NRESX
        IF(VALEUR(IFI,K+KDECAL).NE.-9999.) THEN
          NPTREX(IFI)        = NPTREX(IFI)+1
          A(IFI,NPTREX(IFI)) = VALEUR(IFI,K+KDECAL)
C
C RESEAU DE NRESY VALEURS
C -----------------------
          DO L=(NRESX+KDECAL+1),(NRESX+KDECAL+1-1)+NRESY
            J = NRESX+KDECAL+1+NRESY+(L-NRESX-KDECAL-1)*NRESX+(K-1)
            IF((VALEUR(IFI,L).NE.-9999.).AND.(VALEUR(IFI,J).NE.-9999.))
     &        THEN
              NPTRYR(IFI,K)                    = NPTRYR(IFI,K)+1
              B(IFI,NPTRYR(IFI,K))             = VALEUR(IFI,L)
              C(IFI,NPTREX(IFI),NPTRYR(IFI,K)) = VALEUR(IFI,J)
            ENDIF
          ENDDO 
        ENDIF
      ENDDO
C            
C PAS DE TROUS DANS LE RESEAU
C ---------------------------
      IF(NPTREX(IFI).EQ.0) THEN
  902   FORMAT(1X,'PAS D''ABSCISSES DANS LE RESEAU IFI =',I2)
        WRITE(MESSAG,902) IFI
        CALL ERROR(NOMPB,MESSAG,19)
      ELSE
        DO K=1,NPTREX(IFI)
          IF(NPTRYR(IFI,K).EQ.0) THEN
  903       FORMAT(1X,'PAS D''ORDONNEES POUR a =',E13.6,' ET IFI =',I2)
            WRITE(MESSAG,903) A(IFI,K),IFI
            CALL ERROR(NOMPB,MESSAG,19)
          ELSE
            DO L=K+1,NPTREX(IFI)
              IF(NPTRYR(IFI,K).NE.NPTRYR(IFI,L)) THEN
  904           FORMAT(1X,'NOMBRE DE POINTS c DIFFERENT ',I2,1X,I2,' POU
     &R a =',E13.6,1X,' ET a =',E13.6)
                WRITE(MESSAG,904) NPTRYR(IFI,K),NPTRYR(IFI,L),
     &                            A(IFI,K),A(IFI,L)
                CALL ERROR(NOMPB,MESSAG,19)
              ENDIF
            ENDDO
          ENDIF
        ENDDO
      ENDIF
C
C ECRITURE DU RESEAU
C ------------------
      WRITE(6,*) 'NOMBRE DE POINTS ABSCISSES RESEAU =',NPTREX(IFI)
      DO J=1,NPTREX(IFI)
        NPTREY(IFI) = NPTRYR(IFI,J)
        WRITE(6,*) 'NOMBRE DE POINTS ORDONNEES RESEAU =',NPTREY(IFI)
        DO K=1,NPTREY(IFI)
          WRITE(6,*) 'a,b,c =',A(IFI,J),B(IFI,K),C(IFI,J,K)
        ENDDO
      ENDDO
C
      RETURN
      END
C ---------------------------------------------------------
      SUBROUTINE LECSTR(NOMPB,CHAINE,IU1,IU2,NOMBRE,VALSTR)
C ---------------------------------------------------------
C SOUS PROGRAMME DE LECTURE DES DONNEES STRATEGIE
C ENTREES NOMPB  NOM DU PROBLEME
C         CHAINE TABLEAU DE CHAINES
C         IU1    UNITE DU FICHIER BANQUE
C         IU2    UNITE DU FICHIER prefixe.erreurs
C         NOMBRE NOMBRE DE VALEURS A RECUPERER
C SORTIE  VALSTR TABLEAU DE VALEURS
C *********************************
      CHARACTER*80 NOMPB

	CHARACTER*(*) CHAINE(*)
      CHARACTER*80  CHAIRE,CARTE,MESSAG,CHVAL
      CHARACTER*1 COTE
      REAL VALSTR(*)
C
      WRITE(6,*) 'SOUS PROGRAMME LECSTR'
C
      COTE = '"'
      DO 50 I=1,NOMBRE
        CHAIRE = COTE//CHAINE(I)(IFDEB(CHAINE(I)):IFFIN(CHAINE(I)))//COT
     &E
        REWIND(IU1)
        ICOEF = 0
C
    1   READ(IU1,'(A)',END=2,ERR=900) CARTE
        IPOS1 = INDEX(CARTE,' ')
        IPOS2 = INDEX(CARTE,CHAIRE(IFDEB(CHAIRE):IFFIN(CHAIRE)))
        IF((INDEX(CARTE,'ABRE').NE.0).AND.(IPOS2.NE.0).AND.
     &    (IFFIN(CARTE(IPOS1:IPOS2))-IFDEB(CARTE(IPOS1:IPOS2)).EQ.0))
     &    THEN
          ICOEF  = 1
          L1     = INDEX(CARTE,'"') 
          L2     = INDEX(CARTE(L1+1:80),'"') 
          L3     = INDEX(CARTE(L1+L2+1:80),'"') 
          L4     = INDEX(CARTE(L1+L2+L3+1:80),'"') 
          READ(CARTE(L1+L2+L3+1:L1+L2+L3+L4-1),'(A)') CHVAL
          IF(IFDEB(CHVAL).EQ.0) GOTO 2
          IF(INDEX(CARTE,'ABRETOG').NE.0) THEN
            IF(INDEX(CHVAL,'PAS DE CALCUL').NE.0)        VALSTR(I) = 0
            IF(INDEX(CHVAL,'AMORCAGE').NE.0)             VALSTR(I) = 1
            IF(INDEX(CHVAL,'PROPAGATION').NE.0)          VALSTR(I) = 2
            IF(INDEX(CHVAL,'AMORCAGE+PROPAGATION').NE.0) VALSTR(I) = 3
            IF(INDEX(CHVAL,'GENERATION').NE.0)           VALSTR(I) = 1
            IF(INDEX(CHVAL,'PAS DE GENERATION').NE.0)    VALSTR(I) = 2
            IF(INDEX(CHVAL,'RESEAU').NE.0)               VALSTR(I) = 1
            IF(INDEX(CHVAL,'ELBER').NE.0)                VALSTR(I) = 2
            IF(INDEX(CHVAL,'NASGRO').NE.0)               VALSTR(I) = 3
            IF(INDEX(CHVAL,'MANUEL').NE.0)               VALSTR(I) = 2
            IF(INDEX(CHVAL,'ANCIENNE').NE.0)             VALSTR(I) = 3
            IF(INDEX(CHVAL,'Smax<0').NE.0)               VALSTR(I) = 1
            IF(INDEX(CHVAL,'Smax<0-Smin=0 SI R<0').NE.0) VALSTR(I) = 0
            IF(INDEX(CHVAL,'PAS D''ELIMINATION').NE.0)   VALSTR(I) = 2
            IF(INDEX(CHVAL,'COMPRESSION').NE.0)          VALSTR(I) = 1
            IF(INDEX(CHVAL,'PAS DE COMPRESSION').NE.0)   VALSTR(I) = 2
            IF(INDEX(CHVAL,'PAS DE STOCKAGE').NE.0)      VALSTR(I) = 1
            IF(INDEX(CHVAL,'STOCKAGE').NE.0)             VALSTR(I) = 2
            IF(INDEX(CHVAL,'OUI').NE.0)                  VALSTR(I) = 1
            IF(INDEX(CHVAL,'NON').NE.0)                  VALSTR(I) = 2
            IF(INDEX(CHVAL,'ASRUP').NE.0)                VALSTR(I) = 1
            IF(INDEX(CHVAL,'FENTOMAS').NE.0)             VALSTR(I) = 2
            IF(INDEX(CHVAL,'K EF').NE.0)                 VALSTR(I) = 1
            IF(INDEX(CHVAL,'K ANALYTIQUE').NE.0)         VALSTR(I) = 2
          ELSE
            IF((INDEX(CHVAL,'ASRUP').NE.0).OR.
     &         (INDEX(CHVAL,'K EF').NE.0)) THEN
              VALSTR(I) = 1
            ELSEIF((INDEX(CHVAL,'FENTOMAS').NE.0).OR.
     &             (INDEX(CHVAL,'K ANALYTIQUE').NE.0)) THEN
              VALSTR(I) = 2
            ELSE
              READ(CARTE(L1+L2+L3+1:L1+L2+L3+L4-1),*) VALSTR(I)
            ENDIF
          ENDIF
          GOTO 50
        ENDIF
        GOTO 1
C
    2   CONTINUE
C--------------------------
        IF (CHAIRE(IFDEB(CHAIRE)+1:IFFIN(CHAIRE)-1).EQ.'%CONVERGE')THEN
           VALSTR(I) = 0
           GOTO 50
        ENDIF
C--------------------------
  902   FORMAT(1X,'DONNEE INEXISTANTE = ',A) 
        WRITE(MESSAG,902) CHAIRE(IFDEB(CHAIRE)+1:IFFIN(CHAIRE)-1)
        CALL ERROR(NOMPB,MESSAG,IU2)
   50 CONTINUE
C
      RETURN
C
  901 FORMAT(1X,'ERREUR A LA LECTURE DES DONNEES')   
  900 WRITE(MESSAG,901)
      CALL ERROR(NOMPB,MESSAG,IU2)
      END
